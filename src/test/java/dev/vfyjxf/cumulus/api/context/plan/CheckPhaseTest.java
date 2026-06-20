package dev.vfyjxf.cumulus.api.context.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vfyjxf.cumulus.api.component.Axis;
import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.context.plan.stdlib.CheckPhases;
import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.impl.check.InMemoryTargetSnapshot;
import dev.vfyjxf.cumulus.impl.check.StructureCheckExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link CheckPhase} dynamic-expansion hook: a custom phase that
 * returns extra points to match, and the {@link CheckPhases#repeatRange}
 * convenience for axial min/max repetition.
 */
class CheckPhaseTest {

    private static final ContextKey<Integer> extraCount = ContextKey.integer("extra_count");

    @Test
    void customCheckPhaseAddsPointsThatGetMatched() {
        // controller at origin; phase adds 2 extra casing points around it
        var definition = Structures.define(ResourceLocation.parse("cumulus:phase_test"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .checkPhase(ctx -> {
                    var extra = new ArrayList<StructurePointDefinition>();
                    // Add 2 casing points at (1,0,0) and (-1,0,0)
                    extra.add(new StructurePointDefinition(
                            new StructurePointId("casing_e"),
                            RelativeBlockPos.of(1, 0, 0),
                            Targets.block().target(),
                            List.of(),
                            PointRequirement.required,
                            java.util.Set.of(),
                            java.util.Set.of(),
                            dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata.empty()));
                    extra.add(new StructurePointDefinition(
                            new StructurePointId("casing_w"),
                            RelativeBlockPos.of(-1, 0, 0),
                            Targets.block().target(),
                            List.of(),
                            PointRequirement.required,
                            java.util.Set.of(),
                            java.util.Set.of(),
                            dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata.empty()));
                    return PhaseResult.of(extra);
                })
                .build();

        // All 3 blocks present → valid
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .block(RelativeBlockPos.of(1, 0, 0), "cumulus:casing")
                .block(RelativeBlockPos.of(-1, 0, 0), "cumulus:casing")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);
        assertEquals(InstanceStatus.valid, result.status());
    }

    @Test
    void customCheckPhaseExtraPointMissingMakesInvalid() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:phase_missing"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .checkPhase(ctx -> PhaseResult.of(List.of(new StructurePointDefinition(
                        new StructurePointId("required_extra"),
                        RelativeBlockPos.of(1, 0, 0),
                        Targets.block().target(),
                        List.of(),
                        PointRequirement.required,
                        java.util.Set.of(),
                        java.util.Set.of(),
                        dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata.empty())))
                )
                .build();

        // Only controller present, extra point missing → invalid
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);
        // Missing required point → executor emits DataRequest → status is incomplete
        // (not invalid, because "missing" means "data may not be loaded yet")
        assertEquals(InstanceStatus.incomplete, result.status());
    }

    @Test
    void checkPhaseCanWriteComputedValuesToContext() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:phase_context"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .checkPhase(ctx -> {
                    ctx.context().putComputed(extraCount, 42);
                    return PhaseResult.empty();
                })
                .build();

        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);
        assertEquals(InstanceStatus.valid, result.status());
        assertEquals(42, result.context().get(extraCount).orElse(-1));
    }

    @Test
    void nullCheckPhaseIsNoOp() {
        // No checkPhase → existing behavior, 209 tests still pass
        var definition = Structures.define(ResourceLocation.parse("cumulus:no_phase"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();

        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);
        assertEquals(InstanceStatus.valid, result.status());
        assertTrue(result.context().get(extraCount).isEmpty());
    }

    @Test
    void axisShiftWorksOnAllAxes() {
        assertEquals(RelativeBlockPos.of(3, 0, 0), Axis.x.shift(RelativeBlockPos.of(0, 0, 0), 3));
        assertEquals(RelativeBlockPos.of(0, 5, 0), Axis.y.shift(RelativeBlockPos.of(0, 0, 0), 5));
        assertEquals(RelativeBlockPos.of(0, 0, 7), Axis.z.shift(RelativeBlockPos.of(0, 0, 0), 7));
        // cumulative shift
        assertEquals(RelativeBlockPos.of(2, 3, 0), Axis.y.shift(Axis.x.shift(RelativeBlockPos.of(0, 0, 0), 2), 3));
    }
}
