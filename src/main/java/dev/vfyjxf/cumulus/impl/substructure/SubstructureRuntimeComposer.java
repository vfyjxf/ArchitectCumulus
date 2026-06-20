package dev.vfyjxf.cumulus.impl.substructure;

import dev.vfyjxf.cumulus.api.component.ComponentDescriptor;
import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.context.ContextSources;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.target.TargetDescriptor;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.api.target.types.RegionTargetType;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class SubstructureRuntimeComposer {
    private static final ResourceLocation regionTarget = RegionTargetType.id;

    private SubstructureRuntimeComposer() {}

    public static StructureDefinition compose(ResourceLocation rootDefinitionId, Map<ResourceLocation, StructureDefinition> definitionsById, SubstructureGraph graph) {
        if (rootDefinitionId == null) {
            throw new IllegalArgumentException("Root definition id must not be null");
        }
        if (definitionsById == null) {
            throw new IllegalArgumentException("Structure definitions map must not be null");
        }
        if (graph == null) {
            throw new IllegalArgumentException("Substructure graph must not be null");
        }
        var rootDefinition = definitionsById.get(rootDefinitionId);
        if (rootDefinition == null) {
            throw new IllegalArgumentException("Root definition is missing: " + rootDefinitionId);
        }

        var offsetsByOccurrence = cumulativeOffsets(graph);
        var points = new ArrayList<StructurePointDefinition>();
        var contextRules = new ArrayList<ContextRuleDescriptor<?>>();
        var components = new ArrayList<ComponentDescriptor>();
        for (var node : graph.nodes()) {
            var definition = definitionsById.get(node.definitionId());
            if (definition == null) {
                throw new IllegalArgumentException("Substructure definition is missing: " + node.definitionId());
            }
            var offset = offsetsByOccurrence.get(node.occurrenceId());
            for (var point : definition.points()) {
                points.add(translatePoint(point, node, offset, graph.root().occurrenceId()));
            }
            if (node.occurrenceId().equals(graph.root().occurrenceId())) {
                contextRules.addAll(definition.contextRules());
                components.addAll(definition.components());
            } else {
                contextRules.addAll(translateContextRules(definition.contextRules(), node.occurrenceId()));
            }
        }

        return new StructureDefinition(
                        rootDefinition.id(),
                        rootDefinition.schemaVersion(),
                        rootDefinition.transforms(),
                        points,
                        components,
                        contextRules,
                        rootDefinition.checkPhase(),
                        rootDefinition.versions(),
                        rootDefinition.migrations(),
                        rootDefinition.diagnostics());
    }

    public static List<RuntimeSubstructureOccurrence> occurrences(SubstructureGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Substructure graph must not be null");
        }
        var offsetsByOccurrence = cumulativeOffsets(graph);
        var occurrences = new ArrayList<RuntimeSubstructureOccurrence>();
        for (var node : graph.nodes()) {
            if (node.occurrenceId().equals(graph.root().occurrenceId())) {
                continue;
            }
            occurrences.add(new RuntimeSubstructureOccurrence(
                            node.occurrenceId(),
                            node.definitionId(),
                            node.parentOccurrenceId(),
                            offsetsByOccurrence.get(node.occurrenceId())));
        }
        return List.copyOf(occurrences);
    }

    private static List<ContextRuleDescriptor<?>> translateContextRules(List<ContextRuleDescriptor<?>> rules, String occurrenceId) {
        var translated = new ArrayList<ContextRuleDescriptor<?>>();
        for (var rule : rules) {
            translated.add(translateRule(rule, occurrenceId));
        }
        return translated;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ContextRuleDescriptor<?> translateRule(ContextRuleDescriptor<?> rule, String occurrenceId) {
        var raw = (ContextRuleDescriptor) rule;
        var prefixedKey = ContextKey.string(occurrenceId + "/" + raw.targetKey().name());
        var prefixedSource = ContextSources.prefix(raw.source(), occurrenceId);
        return new ContextRuleDescriptor(prefixedKey, prefixedSource);
    }

    private static String prefixSource(String occurrenceId, String source) {
        var dot = source.indexOf('.');
        if (dot <= 0 || dot == source.length() - 1) {
            return occurrenceId + "/" + source;
        }
        return occurrenceId + "/" + source.substring(0, dot) + "." + source.substring(dot + 1);
    }

    private static StructurePointDefinition translatePoint(StructurePointDefinition point, SubstructureGraphNode node, RelativeBlockPos offset, String rootOccurrenceId) {
        var translatedLocation = translate(point.location(), offset);
        var translatedTarget = translateTarget(point.target(), offset);
        return new StructurePointDefinition(
                        new StructurePointId(pointId(node, point, rootOccurrenceId)),
                        translatedLocation,
                        translatedTarget,
                        point.predicates(),
                        point.requirement(),
                        translatedCaptures(node, point, rootOccurrenceId),
                        translatedGroups(node, point, rootOccurrenceId),
                        point.diagnostics());
    }

    private static String pointId(SubstructureGraphNode node, StructurePointDefinition point, String rootOccurrenceId) {
        if (node.occurrenceId().equals(rootOccurrenceId)) {
            return point.id().value();
        }
        return node.occurrenceId() + "/" + point.id().value();
    }

    private static LinkedHashSet<String> translatedCaptures(SubstructureGraphNode node, StructurePointDefinition point, String rootOccurrenceId) {
        var captures = new LinkedHashSet<String>();
        for (var capture : point.captures()) {
            if (node.occurrenceId().equals(rootOccurrenceId)) {
                captures.add(capture);
            } else {
                captures.add(node.occurrenceId() + "/" + capture);
            }
        }
        return captures;
    }

    private static LinkedHashSet<String> translatedGroups(SubstructureGraphNode node, StructurePointDefinition point, String rootOccurrenceId) {
        var groups = new LinkedHashSet<String>();
        for (var group : point.groups()) {
            if (node.occurrenceId().equals(rootOccurrenceId)) {
                groups.add(group);
            } else {
                groups.add(node.occurrenceId() + "/" + group);
            }
        }
        return groups;
    }

    private static TargetDescriptor translateTarget(TargetDescriptor target, RelativeBlockPos offset) {
        if (!regionTarget.equals(target.typeId())) {
            return target;
        }
        var properties = new LinkedHashMap<>(target.properties());
        properties.computeIfPresent("from", (ignored, value) -> translate(parsePos(value), offset).asString());
        properties.computeIfPresent("to", (ignored, value) -> translate(parsePos(value), offset).asString());
        return new TargetDescriptor(target.typeId(), properties);
    }

    private static Map<String, RelativeBlockPos> cumulativeOffsets(SubstructureGraph graph) {
        var offsets = new LinkedHashMap<String, RelativeBlockPos>();
        offsets.put(graph.root().occurrenceId(), RelativeBlockPos.of(0, 0, 0));
        var nodesByOccurrence = graph.nodesByOccurrenceId();
        for (var node : graph.nodes()) {
            if (node.occurrenceId().equals(graph.root().occurrenceId())) {
                continue;
            }
            var parent = nodesByOccurrence.get(node.parentOccurrenceId());
            if (parent == null) {
                throw new IllegalArgumentException("Substructure parent is missing: " + node.parentOccurrenceId());
            }
            var parentOffset = offsets.get(parent.occurrenceId());
            if (parentOffset == null) {
                throw new IllegalArgumentException("Substructure parent offset is missing: " + parent.occurrenceId());
            }
            offsets.put(node.occurrenceId(), translate(node.anchorOffsetFromParent(), parentOffset));
        }
        return offsets;
    }

    private static RelativeBlockPos translate(RelativeBlockPos position, RelativeBlockPos offset) {
        return RelativeBlockPos.of(position.x() + offset.x(), position.y() + offset.y(), position.z() + offset.z());
    }

    private static RelativeBlockPos parsePos(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Relative position string must not be blank");
        }
        var parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Relative position must have x,y,z parts");
        }
        return RelativeBlockPos.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
