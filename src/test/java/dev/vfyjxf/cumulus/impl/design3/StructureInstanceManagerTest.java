package dev.vfyjxf.cumulus.impl.design3;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.ownership.ClaimMode;
import dev.vfyjxf.cumulus.api.ownership.PartClaim;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.RequestReason;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.design3.check.InMemoryTargetSnapshot;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureInstanceManagerTest {
    @Test
    void managerRegistersPlanAndRequestsFootprintSections() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .component(Components.region("body", RelativeBlockPos.of(16, 0, 0), RelativeBlockPos.of(17, 0, 0)))
                .build();
        var manager = new StructureInstanceManager();
        manager.register(StructureRuntimePlanCompiler.compile(definition));

        var frame = manager.track(definition.id(), new BlockPos(0, 64, 0));

        assertEquals(2, frame.requests().size());
        assertEquals(RequestReason.PREFETCH_INCOMPLETE_INSTANCE, frame.requests().getFirst().reason());
        assertEquals(new SectionKey(0, 4, 0), frame.requests().getFirst().section());
        assertEquals(InstanceStatus.INCOMPLETE, frame.deltas().getFirst().status());
    }

    @Test
    void managerPublishesVersionedRuntimeContextDelta() {
        var definitionId = ResourceLocation.parse("cumulus:machine");
        var anchor = new BlockPos(1, 64, 1);
        var manager = new StructureInstanceManager();
        manager.register(new StructureRuntimePlan(definitionId, java.util.List.of(), java.util.List.of(), java.util.List.of(),
                new dev.vfyjxf.cumulus.api.visualization.VisualizationModel("empty", java.util.List.of())));
        manager.track(definitionId, anchor);

        var frame = manager.publishContext(definitionId, anchor, Map.of("facing", "north"));

        assertEquals(1, frame.deltas().size());
        assertEquals(InstanceStatus.VALID, frame.deltas().getFirst().status());
        assertEquals(1, frame.deltas().getFirst().context().revision());
        assertEquals("north", frame.deltas().getFirst().context().values().get("facing"));
    }

    @Test
    void managerRunsCheckAndPublishesResultFrame() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors.id("cumulus:controller"))
                        .capture("controller"))
                .build();
        var anchor = new BlockPos(4, 64, 4);
        var manager = new StructureInstanceManager();
        manager.register(StructureRuntimePlanCompiler.compile(definition));

        var frame = manager.check(
                definition,
                anchor,
                InMemoryTargetSnapshot.builder()
                        .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                        .build());

        assertEquals(InstanceStatus.VALID, frame.deltas().getFirst().status());
        assertEquals("4,64,4", frame.deltas().getFirst().context().values().get("controller"));
        assertEquals(0, frame.requests().size());
    }

    @Test
    void exclusiveClaimsConflictWithExistingExclusiveOwnerAndProduceDiagnostics() {
        var definition = validControllerDefinition(ResourceLocation.parse("cumulus:machine"));
        var part = ResourceLocation.parse("cumulus:parts/controller");
        var firstAnchor = new BlockPos(0, 64, 0);
        var secondAnchor = new BlockPos(8, 64, 0);
        var first = new StructureInstanceKey(definition.id(), firstAnchor);
        var second = new StructureInstanceKey(definition.id(), secondAnchor);
        var manager = registeredManager(definition);

        manager.check(definition, firstAnchor, validControllerSnapshot(), List.of(
                new PartClaim(part, first, ClaimMode.EXCLUSIVE)));

        var frame = manager.check(definition, secondAnchor, validControllerSnapshot(), List.of(
                new PartClaim(part, second, ClaimMode.EXCLUSIVE)));

        assertEquals(InstanceStatus.INVALID, frame.deltas().getFirst().status());
        var conflict = manager.diagnostics(second).childrenOf("root").getFirst();
        assertEquals("ownership-conflict", conflict.id());
        assertEquals(Severity.ERROR, conflict.severity());
        assertEquals("OWNERSHIP_CONFLICT", conflict.evidence().getFirst().detail());
    }

    @Test
    void sharedClaimsRemainCompatibleWithExistingSharedOwners() {
        var definition = validControllerDefinition(ResourceLocation.parse("cumulus:shared_machine"));
        var part = ResourceLocation.parse("cumulus:parts/casing");
        var firstAnchor = new BlockPos(0, 64, 0);
        var secondAnchor = new BlockPos(8, 64, 0);
        var first = new StructureInstanceKey(definition.id(), firstAnchor);
        var second = new StructureInstanceKey(definition.id(), secondAnchor);
        var manager = registeredManager(definition);

        manager.check(definition, firstAnchor, validControllerSnapshot(), List.of(
                new PartClaim(part, first, ClaimMode.SHARED)));

        var frame = manager.check(definition, secondAnchor, validControllerSnapshot(), List.of(
                new PartClaim(part, second, ClaimMode.SHARED)));

        assertEquals(InstanceStatus.VALID, frame.deltas().getFirst().status());
        assertTrue(manager.diagnostics(second).nodes().isEmpty());
    }

    @Test
    void exclusiveClaimsConflictWithExistingSharedOwnersAndProduceDiagnostics() {
        var definition = validControllerDefinition(ResourceLocation.parse("cumulus:mixed_machine"));
        var part = ResourceLocation.parse("cumulus:parts/shared_bus");
        var firstAnchor = new BlockPos(0, 64, 0);
        var secondAnchor = new BlockPos(8, 64, 0);
        var first = new StructureInstanceKey(definition.id(), firstAnchor);
        var second = new StructureInstanceKey(definition.id(), secondAnchor);
        var manager = registeredManager(definition);

        manager.check(definition, firstAnchor, validControllerSnapshot(), List.of(
                new PartClaim(part, first, ClaimMode.SHARED)));

        var frame = manager.check(definition, secondAnchor, validControllerSnapshot(), List.of(
                new PartClaim(part, second, ClaimMode.EXCLUSIVE)));

        assertEquals(InstanceStatus.INVALID, frame.deltas().getFirst().status());
        var evidence = manager.diagnostics(second).childrenOf("root").getFirst().evidence().getFirst();
        assertEquals("OWNERSHIP_CONFLICT", evidence.detail());
        assertEquals(first.toString(), evidence.descriptors().get("existing.owner"));
        assertEquals(ClaimMode.SHARED.name(), evidence.descriptors().get("existing.mode"));
        assertEquals(ClaimMode.EXCLUSIVE.name(), evidence.descriptors().get("attempted.mode"));
    }

    @Test
    void invalidChecksDoNotRegisterOwnershipClaims() {
        var definition = validControllerDefinition(ResourceLocation.parse("cumulus:invalid_claim"));
        var part = ResourceLocation.parse("cumulus:parts/controller");
        var invalidAnchor = new BlockPos(0, 64, 0);
        var validAnchor = new BlockPos(8, 64, 0);
        var invalid = new StructureInstanceKey(definition.id(), invalidAnchor);
        var valid = new StructureInstanceKey(definition.id(), validAnchor);
        var manager = registeredManager(definition);

        var invalidFrame = manager.check(definition, invalidAnchor, InMemoryTargetSnapshot.empty(), List.of(
                new PartClaim(part, invalid, ClaimMode.EXCLUSIVE)));
        var validFrame = manager.check(definition, validAnchor, validControllerSnapshot(), List.of(
                new PartClaim(part, valid, ClaimMode.EXCLUSIVE)));

        assertEquals(InstanceStatus.INCOMPLETE, invalidFrame.deltas().getFirst().status());
        assertEquals(InstanceStatus.VALID, validFrame.deltas().getFirst().status());
        assertTrue(manager.diagnostics(valid).nodes().isEmpty());
    }

    @Test
    void conflictingClaimBatchDoesNotPartiallyRegisterEarlierClaims() {
        var definition = validControllerDefinition(ResourceLocation.parse("cumulus:batch_conflict"));
        var existingPart = ResourceLocation.parse("cumulus:parts/existing");
        var newPart = ResourceLocation.parse("cumulus:parts/new");
        var firstAnchor = new BlockPos(0, 64, 0);
        var secondAnchor = new BlockPos(8, 64, 0);
        var thirdAnchor = new BlockPos(16, 64, 0);
        var first = new StructureInstanceKey(definition.id(), firstAnchor);
        var second = new StructureInstanceKey(definition.id(), secondAnchor);
        var third = new StructureInstanceKey(definition.id(), thirdAnchor);
        var manager = registeredManager(definition);

        manager.check(definition, firstAnchor, validControllerSnapshot(), List.of(
                new PartClaim(existingPart, first, ClaimMode.EXCLUSIVE)));
        var conflictFrame = manager.check(definition, secondAnchor, validControllerSnapshot(), List.of(
                new PartClaim(newPart, second, ClaimMode.EXCLUSIVE),
                new PartClaim(existingPart, second, ClaimMode.EXCLUSIVE)));
        var thirdFrame = manager.check(definition, thirdAnchor, validControllerSnapshot(), List.of(
                new PartClaim(newPart, third, ClaimMode.EXCLUSIVE)));

        assertEquals(InstanceStatus.INVALID, conflictFrame.deltas().getFirst().status());
        assertEquals(InstanceStatus.VALID, thirdFrame.deltas().getFirst().status());
        assertTrue(manager.diagnostics(third).nodes().isEmpty());
    }

    private static StructureInstanceManager registeredManager(dev.vfyjxf.cumulus.api.definition.StructureDefinition definition) {
        var manager = new StructureInstanceManager();
        manager.register(StructureRuntimePlanCompiler.compile(definition));
        return manager;
    }

    private static dev.vfyjxf.cumulus.api.definition.StructureDefinition validControllerDefinition(ResourceLocation id) {
        return Structures.define(id)
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors.id("cumulus:controller")))
                .build();
    }

    private static InMemoryTargetSnapshot validControllerSnapshot() {
        return InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .build();
    }
}
