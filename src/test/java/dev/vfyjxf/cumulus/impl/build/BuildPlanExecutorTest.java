package dev.vfyjxf.cumulus.impl.build;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.target.TargetDescriptor;
import dev.vfyjxf.cumulus.api.target.TargetPattern;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPlanExecutorTest {
    @Test
    void generatesDeterministicPointAndRegionBuildActionsFromRuntimePlan() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:buildable_machine")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()).point("port", RelativeBlockPos.of(2, 0, 0), Targets.blockEntity()).point("coolant", RelativeBlockPos.of(3, 0, 0), Targets.fluid()).component(Components.region("frame", RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 2, 0))).build();
        var plan = StructureRuntimePlanCompiler.compile(definition);

        var actions = BuildPlanExecutor.actions(plan);

        assertEquals(List.of(new BuildAction(
                        "controller",
                        BuildActionKind.point,
                        ResourceLocation.parse("cumulus:block"),
                        RelativeBlockPos.of(0, 0, 0),
                        RelativeBlockPos.of(0, 0, 0)), new BuildAction(
                                        "port",
                                        BuildActionKind.point,
                                        ResourceLocation.parse("cumulus:block_entity"),
                                        RelativeBlockPos.of(2, 0, 0),
                                        RelativeBlockPos.of(2, 0, 0)), new BuildAction(
                                                        "coolant",
                                                        BuildActionKind.point,
                                                        ResourceLocation.parse("cumulus:fluid"),
                                                        RelativeBlockPos.of(3, 0, 0),
                                                        RelativeBlockPos.of(3, 0, 0)), new BuildAction(
                                                                        "frame",
                                                                        BuildActionKind.region,
                                                                        ResourceLocation.parse("cumulus:region"),
                                                                        RelativeBlockPos.of(-1, 0, 0),
                                                                        RelativeBlockPos.of(1, 2, 0),
                                                                        Map.of("from", "-1,0,0", "to", "1,2,0"))), actions);
    }

    @Test
    void buildActionsPreserveTargetPropertiesForBlockEntityPlacement() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:block_entity_buildable")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.blockEntity(ResourceLocation.parse("minecraft:furnace"), ResourceLocation.parse("minecraft:furnace"), "{Items:[]}")).build();

        var action = BuildPlanExecutor.actions(StructureRuntimePlanCompiler.compile(definition)).getFirst();

        assertEquals(ResourceLocation.parse("cumulus:block_entity"), action.targetType());
        assertEquals(Map.of("block", "minecraft:furnace", "block_entity", "minecraft:furnace", "nbt", "{Items:[]}"), action.properties());
    }

    @Test
    void buildActionsPreserveTargetPropertiesForEntitySpawning() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:entity_buildable")).point("worker", RelativeBlockPos.of(0, 1, 0), Targets.entity(ResourceLocation.parse("minecraft:villager"), "{NoAI:1b}")).build();

        var action = BuildPlanExecutor.actions(StructureRuntimePlanCompiler.compile(definition)).getFirst();

        assertEquals(ResourceLocation.parse("cumulus:entity"), action.targetType());
        assertEquals(Map.of("entity", "minecraft:villager", "nbt", "{NoAI:1b}"), action.properties());
    }

    @Test
    void buildActionsPreserveTargetPropertiesForCapabilityBackingBlockPlacement() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:capability_buildable")).point("energy", RelativeBlockPos.of(0, 0, 0), Targets.capability("energy").property("block", "minecraft:furnace").property("side", "north")).build();

        var action = BuildPlanExecutor.actions(StructureRuntimePlanCompiler.compile(definition)).getFirst();

        assertEquals(ResourceLocation.parse("cumulus:capability"), action.targetType());
        assertEquals(Map.of("name", "energy", "block", "minecraft:furnace", "side", "north"), action.properties());
    }

    @Test
    void appliesCapabilityBackingBlockActionsToInMemoryTargetAndIsIdempotent() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:capability_build_action")).point("energy", RelativeBlockPos.of(0, 0, 0), Targets.capability("energy").property("block", "minecraft:furnace")).build();
        var target = new InMemoryBuildTarget();
        var firstResult = BuildPlanExecutor.apply(StructureRuntimePlanCompiler.compile(definition), target);
        var secondResult = BuildPlanExecutor.apply(StructureRuntimePlanCompiler.compile(definition), target);

        assertEquals(BuildPlanStatus.applied, firstResult.status());
        assertEquals(BuildPlanStatus.noop, secondResult.status());
        assertEquals(Set.of(new BuiltTarget(
                        ResourceLocation.parse("cumulus:capability"),
                        RelativeBlockPos.of(0, 0, 0),
                        RelativeBlockPos.of(0, 0, 0),
                        "energy")), Set.copyOf(target.targets().values()));
    }

    @Test
    void appliesCapabilityMutationActionsToInMemoryTargetAndIsIdempotent() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:capability_mutation_build_action")).point("energy", RelativeBlockPos.of(0, 0, 0), Targets.capability("energy").property("operation", "insert").property("resource", "minecraft:coal").property("amount", "8")).build();
        var target = new InMemoryBuildTarget();
        var firstResult = BuildPlanExecutor.apply(StructureRuntimePlanCompiler.compile(definition), target);
        var secondResult = BuildPlanExecutor.apply(StructureRuntimePlanCompiler.compile(definition), target);

        assertEquals(BuildPlanStatus.applied, firstResult.status());
        assertEquals(BuildPlanStatus.noop, secondResult.status());
        assertEquals(Map.of("operation", "insert", "resource", "minecraft:coal", "amount", "8"), target.capabilityMutations().get("cumulus:capability|0,0,0|0,0,0|energy"));
    }

    @Test
    void capabilityBuildActionWithoutBackingBlockReturnsDiagnosticAndDoesNotMutateTarget() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:unsupported_capability_build")).point("energy", RelativeBlockPos.of(0, 0, 0), Targets.capability("energy")).build();
        var target = new InMemoryBuildTarget();

        var result = BuildPlanExecutor.apply(StructureRuntimePlanCompiler.compile(definition), target);

        assertEquals(BuildPlanStatus.unsupportedTarget, result.status());
        assertTrue(target.targets().isEmpty());
        var diagnostic = result.diagnostics().childrenOf("root").getFirst();
        assertEquals("energy", diagnostic.id());
        assertEquals("UNSUPPORTED_BUILD_TARGET", diagnostic.evidence().getFirst().detail());
        assertEquals("cumulus:capability", diagnostic.evidence().getFirst().targetId());
    }

    @Test
    void appliesActionsToInMemoryTargetAndIsIdempotent() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:in_memory_machine")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()).component(Components.region("frame", RelativeBlockPos.of(0, 0, 1), RelativeBlockPos.of(1, 1, 1))).build();
        var target = new InMemoryBuildTarget();
        var firstResult = BuildPlanExecutor.apply(StructureRuntimePlanCompiler.compile(definition), target);
        var firstSnapshot = target.targets();

        var secondResult = BuildPlanExecutor.apply(StructureRuntimePlanCompiler.compile(definition), target);

        assertEquals(BuildPlanStatus.applied, firstResult.status());
        assertEquals(BuildPlanStatus.noop, secondResult.status());
        assertEquals(firstSnapshot, target.targets());
        assertEquals(Set.of(new BuiltTarget(
                        ResourceLocation.parse("cumulus:block"),
                        RelativeBlockPos.of(0, 0, 0),
                        RelativeBlockPos.of(0, 0, 0),
                        "controller"), new BuiltTarget(
                                        ResourceLocation.parse("cumulus:region"),
                                        RelativeBlockPos.of(0, 0, 1),
                                        RelativeBlockPos.of(1, 1, 1),
                                        "frame")), Set.copyOf(target.targets().values()));
        assertTrue(firstResult.diagnostics().nodes().isEmpty());
        assertTrue(secondResult.diagnostics().nodes().isEmpty());
    }

    @Test
    void unsupportedTargetReturnsDiagnosticAndDoesNotMutateTarget() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:unsupported_build_target")).point("energy", RelativeBlockPos.of(0, 0, 0), new TargetPattern(
                        new TargetDescriptor(ResourceLocation.parse("cumulus:unknown_target"), Map.of("name", "energy")),
                        List.of(),
                        Set.of())).build();
        var target = new InMemoryBuildTarget();

        var result = BuildPlanExecutor.apply(StructureRuntimePlanCompiler.compile(definition), target);

        assertEquals(BuildPlanStatus.unsupportedTarget, result.status());
        assertTrue(target.targets().isEmpty());
        assertEquals("root", result.diagnostics().rootNodeId());
        assertEquals(2, result.diagnostics().nodes().size());
        var diagnostic = result.diagnostics().childrenOf("root").getFirst();
        assertEquals("energy", diagnostic.id());
        assertEquals(Severity.error, diagnostic.severity());
        assertEquals("UNSUPPORTED_BUILD_TARGET", diagnostic.evidence().getFirst().detail());
        assertEquals("cumulus:unknown_target", diagnostic.evidence().getFirst().targetId());
    }
}
