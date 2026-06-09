package dev.vfyjxf.cumulus.impl.substructure;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubstructureGraphResolverTest {
    @Test
    void resolvesNestedSubstructureGraphWithOffsetsAndDepth() {
        var parentId = ResourceLocation.parse("cumulus:factory");
        var armId = ResourceLocation.parse("cumulus:arm");
        var gripperId = ResourceLocation.parse("cumulus:gripper");
        var parent = structure(parentId,
                child("left_arm", armId, RelativeBlockPos.of(2, 0, 0)),
                child("right_arm", armId, RelativeBlockPos.of(-2, 0, 0)));
        var arm = structure(armId,
                child("gripper", gripperId, RelativeBlockPos.of(0, 1, 0)));
        var gripper = structure(gripperId);

        var result = SubstructureGraphResolver.resolve(parentId, List.of(parent, arm, gripper));

        assertFalse(result.hasErrors());
        var graph = result.graph().orElseThrow();
        assertEquals(parentId, graph.root().definitionId());
        assertEquals(5, graph.nodes().size());
        assertEquals(List.of(
                        new SubstructureGraphEdge(
                                graph.root().occurrenceId(),
                                "cumulus:factory/left_arm",
                                "left_arm",
                                armId,
                                RelativeBlockPos.of(2, 0, 0)),
                        new SubstructureGraphEdge(
                                "cumulus:factory/left_arm",
                                "cumulus:factory/left_arm/gripper",
                                "gripper",
                                gripperId,
                                RelativeBlockPos.of(0, 1, 0)),
                        new SubstructureGraphEdge(
                                graph.root().occurrenceId(),
                                "cumulus:factory/right_arm",
                                "right_arm",
                                armId,
                                RelativeBlockPos.of(-2, 0, 0)),
                        new SubstructureGraphEdge(
                                "cumulus:factory/right_arm",
                                "cumulus:factory/right_arm/gripper",
                                "gripper",
                                gripperId,
                                RelativeBlockPos.of(0, 1, 0))),
                graph.edges());
        assertEquals(0, graph.root().depth());
        assertEquals(1, graph.node("cumulus:factory/left_arm").orElseThrow().depth());
        assertEquals(2, graph.node("cumulus:factory/left_arm/gripper").orElseThrow().depth());
        assertEquals(RelativeBlockPos.of(2, 0, 0),
                graph.node("cumulus:factory/left_arm").orElseThrow().anchorOffsetFromParent());
        assertEquals(RelativeBlockPos.of(0, 1, 0),
                graph.node("cumulus:factory/left_arm/gripper").orElseThrow().anchorOffsetFromParent());
    }

    @Test
    void reportsDiagnosticWhenChildDefinitionIsMissing() {
        var parentId = ResourceLocation.parse("cumulus:factory");
        var missingId = ResourceLocation.parse("cumulus:missing_child");
        var parent = structure(parentId,
                child("missing", missingId, RelativeBlockPos.of(0, 2, 0)));

        var result = SubstructureGraphResolver.resolve(parentId, List.of(parent));

        assertTrue(result.hasErrors());
        assertTrue(result.graph().isEmpty());
        var diagnostic = result.diagnostics().childrenOf("root").getFirst();
        assertEquals("missing-child-definition", diagnostic.id());
        assertEquals(Severity.ERROR, diagnostic.severity());
        var evidence = diagnostic.evidence().getFirst();
        assertEquals("MISSING_SUBSTRUCTURE_DEFINITION", evidence.detail());
        assertEquals("cumulus:missing_child", evidence.descriptors().get("missing.definition"));
        assertEquals("cumulus:factory/missing", evidence.componentPath());
        assertEquals("0,2,0", evidence.descriptors().get("anchor_offset"));
    }

    @Test
    void reportsDiagnosticWhenSubstructureDefinitionsContainCycle() {
        var parentId = ResourceLocation.parse("cumulus:factory");
        var armId = ResourceLocation.parse("cumulus:arm");
        var parent = structure(parentId,
                child("arm", armId, RelativeBlockPos.of(1, 0, 0)));
        var arm = structure(armId,
                child("parent_again", parentId, RelativeBlockPos.of(0, 0, 1)));

        var result = SubstructureGraphResolver.resolve(parentId, Map.of(
                parent.id(), parent,
                arm.id(), arm));

        assertTrue(result.hasErrors());
        assertTrue(result.graph().isEmpty());
        var diagnostic = result.diagnostics().childrenOf("root").getFirst();
        assertEquals("substructure-cycle", diagnostic.id());
        assertEquals(Severity.ERROR, diagnostic.severity());
        var evidence = diagnostic.evidence().getFirst();
        assertEquals("SUBSTRUCTURE_CYCLE", evidence.detail());
        assertEquals("cumulus:factory -> cumulus:arm -> cumulus:factory", evidence.descriptors().get("cycle"));
        assertEquals("cumulus:factory/arm/parent_again", evidence.componentPath());
    }

    private static StructureDefinition structure(ResourceLocation id, dev.vfyjxf.cumulus.api.component.SubstructureComponent... children) {
        var builder = Structures.define(id);
        for (var child : children) {
            builder.component(child);
        }
        return builder.build();
    }

    private static dev.vfyjxf.cumulus.api.component.SubstructureComponent child(
            String name,
            ResourceLocation childId,
            RelativeBlockPos anchorOffset) {
        return Components.substructure(name, childId, anchorOffset);
    }
}
