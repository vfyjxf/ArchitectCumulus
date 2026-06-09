package dev.vfyjxf.cumulus.impl.design3.substructure;

import dev.vfyjxf.cumulus.api.component.ComponentDescriptor;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class SubstructureGraphResolver {
    private static final ResourceLocation SUBSTRUCTURE_COMPONENT = ResourceLocation.parse("cumulus:substructure");
    private static final String ROOT_NODE_ID = "root";
    private static final RelativeBlockPos ROOT_OFFSET = RelativeBlockPos.of(0, 0, 0);

    private SubstructureGraphResolver() {
    }

    public static SubstructureGraphResolution resolve(
            ResourceLocation rootDefinitionId,
            Collection<StructureDefinition> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("Substructure definitions must not be null");
        }
        var definitionsById = new LinkedHashMap<ResourceLocation, StructureDefinition>();
        for (var definition : definitions) {
            if (definition == null) {
                throw new IllegalArgumentException("Substructure definition must not be null");
            }
            definitionsById.put(definition.id(), definition);
        }
        return resolve(rootDefinitionId, definitionsById);
    }

    public static SubstructureGraphResolution resolve(
            ResourceLocation rootDefinitionId,
            Map<ResourceLocation, StructureDefinition> definitionsById) {
        if (rootDefinitionId == null) {
            throw new IllegalArgumentException("Substructure root definition id must not be null");
        }
        if (definitionsById == null) {
            throw new IllegalArgumentException("Substructure definitions map must not be null");
        }

        var diagnostics = new DiagnosticAccumulator();
        var rootDefinition = definitionsById.get(rootDefinitionId);
        if (rootDefinition == null) {
            diagnostics.addMissingRoot(rootDefinitionId);
            return SubstructureGraphResolution.failed(diagnostics.toGraph());
        }

        var nodes = new ArrayList<SubstructureGraphNode>();
        var edges = new ArrayList<SubstructureGraphEdge>();
        var root = new SubstructureGraphNode(
                rootDefinitionId.toString(),
                rootDefinitionId,
                null,
                rootDefinitionId.toString(),
                ROOT_OFFSET,
                0);
        nodes.add(root);

        resolveChildren(rootDefinition, root, definitionsById, nodes, edges, new ArrayDeque<>(List.of(rootDefinitionId)), diagnostics);
        if (diagnostics.hasErrors()) {
            return SubstructureGraphResolution.failed(diagnostics.toGraph());
        }
        return SubstructureGraphResolution.resolved(new SubstructureGraph(root, nodes, edges), diagnostics.toGraph());
    }

    private static void resolveChildren(
            StructureDefinition parentDefinition,
            SubstructureGraphNode parentNode,
            Map<ResourceLocation, StructureDefinition> definitionsById,
            List<SubstructureGraphNode> nodes,
            List<SubstructureGraphEdge> edges,
            ArrayDeque<ResourceLocation> definitionStack,
            DiagnosticAccumulator diagnostics) {
        for (var component : parentDefinition.components()) {
            if (!SUBSTRUCTURE_COMPONENT.equals(component.typeId())) {
                continue;
            }

            var childDefinitionId = readDefinitionId(component);
            var anchorOffset = readAnchorOffset(component);
            var childOccurrenceId = parentNode.occurrenceId() + "/" + component.name();
            if (definitionStack.contains(childDefinitionId)) {
                diagnostics.addCycle(childOccurrenceId, component, anchorOffset, definitionStack, childDefinitionId);
                continue;
            }

            var childDefinition = definitionsById.get(childDefinitionId);
            if (childDefinition == null) {
                diagnostics.addMissingChild(childOccurrenceId, component, childDefinitionId, anchorOffset);
                continue;
            }

            var childNode = new SubstructureGraphNode(
                    childOccurrenceId,
                    childDefinitionId,
                    parentNode.occurrenceId(),
                    component.name(),
                    anchorOffset,
                    parentNode.depth() + 1);
            nodes.add(childNode);
            edges.add(new SubstructureGraphEdge(
                    parentNode.occurrenceId(),
                    childOccurrenceId,
                    component.name(),
                    childDefinitionId,
                    anchorOffset));

            definitionStack.addLast(childDefinitionId);
            resolveChildren(childDefinition, childNode, definitionsById, nodes, edges, definitionStack, diagnostics);
            definitionStack.removeLast();
        }
    }

    private static ResourceLocation readDefinitionId(ComponentDescriptor component) {
        var value = component.properties().get("structure");
        if (value instanceof ResourceLocation id) {
            return id;
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return ResourceLocation.parse(stringValue);
        }
        throw new IllegalArgumentException("Substructure descriptor is missing structure property: " + component.name());
    }

    private static RelativeBlockPos readAnchorOffset(ComponentDescriptor component) {
        var value = component.properties().get("anchor_offset");
        if (value instanceof RelativeBlockPos offset) {
            return offset;
        }
        if (value instanceof String stringValue) {
            return parsePos(stringValue);
        }
        throw new IllegalArgumentException("Substructure descriptor is missing anchor_offset property: " + component.name());
    }

    private static RelativeBlockPos parsePos(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Relative position string must not be blank");
        }
        var parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Relative position must have x,y,z parts");
        }
        return RelativeBlockPos.of(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
    }

    private static final class DiagnosticAccumulator {
        private final List<DiagnosticNode> nodes = new ArrayList<>();
        private final List<DiagnosticEdge> edges = new ArrayList<>();

        private DiagnosticAccumulator() {
            nodes.add(new DiagnosticNode(
                    ROOT_NODE_ID,
                    Severity.ERROR,
                    "Substructure graph could not be resolved",
                    "diagnostic.cumulus.substructure_resolution_failed",
                    List.of(),
                    "substructures",
                    List.of()));
        }

        private void addMissingRoot(ResourceLocation rootDefinitionId) {
            addChild(new DiagnosticNode(
                    "missing-root-definition",
                    Severity.ERROR,
                    "Root structure definition is missing",
                    "diagnostic.cumulus.missing_root_definition",
                    List.of(DiagnosticEvidence.relative(
                            ROOT_OFFSET,
                            rootDefinitionId.toString(),
                            "cumulus:substructure_graph",
                            rootDefinitionId.toString(),
                            "MISSING_ROOT_DEFINITION",
                            Map.of("missing.definition", rootDefinitionId.toString()))),
                    "substructures/" + rootDefinitionId,
                    List.of()));
        }

        private void addMissingChild(
                String childOccurrenceId,
                ComponentDescriptor component,
                ResourceLocation childDefinitionId,
                RelativeBlockPos anchorOffset) {
            addChild(new DiagnosticNode(
                    "missing-child-definition",
                    Severity.ERROR,
                    "Substructure child definition is missing",
                    "diagnostic.cumulus.missing_substructure_definition",
                    List.of(DiagnosticEvidence.relative(
                            anchorOffset,
                            childDefinitionId.toString(),
                            SUBSTRUCTURE_COMPONENT.toString(),
                            childOccurrenceId,
                            "MISSING_SUBSTRUCTURE_DEFINITION",
                            Map.of(
                                    "component", component.name(),
                                    "missing.definition", childDefinitionId.toString(),
                                    "anchor_offset", anchorOffset.asString()))),
                    "substructures/" + childOccurrenceId,
                    List.of()));
        }

        private void addCycle(
                String childOccurrenceId,
                ComponentDescriptor component,
                RelativeBlockPos anchorOffset,
                ArrayDeque<ResourceLocation> definitionStack,
                ResourceLocation repeatedDefinitionId) {
            var cycle = new ArrayList<String>();
            var inCycle = false;
            for (var definitionId : definitionStack) {
                if (definitionId.equals(repeatedDefinitionId)) {
                    inCycle = true;
                }
                if (inCycle) {
                    cycle.add(definitionId.toString());
                }
            }
            cycle.add(repeatedDefinitionId.toString());
            addChild(new DiagnosticNode(
                    "substructure-cycle",
                    Severity.ERROR,
                    "Substructure definitions contain a cycle",
                    "diagnostic.cumulus.substructure_cycle",
                    List.of(DiagnosticEvidence.relative(
                            anchorOffset,
                            repeatedDefinitionId.toString(),
                            SUBSTRUCTURE_COMPONENT.toString(),
                            childOccurrenceId,
                            "SUBSTRUCTURE_CYCLE",
                            Map.of(
                                    "component", component.name(),
                                    "cycle", String.join(" -> ", cycle),
                                    "anchor_offset", anchorOffset.asString()))),
                    "substructures/" + childOccurrenceId,
                    List.of()));
        }

        private boolean hasErrors() {
            return nodes.stream()
                    .filter(node -> !node.id().equals(ROOT_NODE_ID))
                    .anyMatch(node -> node.severity().ordinal() >= Severity.ERROR.ordinal());
        }

        private DiagnosticGraph toGraph() {
            if (nodes.size() == 1) {
                return new DiagnosticGraph(ROOT_NODE_ID, List.of(), List.of());
            }
            return new DiagnosticGraph(ROOT_NODE_ID, nodes, edges);
        }

        private void addChild(DiagnosticNode node) {
            nodes.add(node);
            edges.add(new DiagnosticEdge(ROOT_NODE_ID, node.id(), "caused_by"));
        }
    }
}
