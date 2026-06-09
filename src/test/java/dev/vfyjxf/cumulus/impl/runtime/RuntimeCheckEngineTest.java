package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;
import dev.vfyjxf.cumulus.impl.check.TargetSnapshotSource;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCheckEngineTest {
    private static final ResourceLocation BLOCK = ResourceLocation.parse("cumulus:block");

    @Test
    void dirtySectionsRunStructureChecksAndPublishDeltas() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:runtime_engine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller"))
                        .capture("controller"))
                .build();
        var engine = new RuntimeCheckEngine()
                .register(definition)
                .track(definition.id(), new BlockPos(0, 64, 0), source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(10, 4);

        assertEquals(1, frame.deltas().size());
        assertEquals(InstanceStatus.VALID, frame.deltas().getFirst().status());
        assertEquals("0,64,0", frame.deltas().getFirst().context().values().get("controller"));
        assertEquals(List.of(), frame.requests());
    }

    @Test
    void frameBudgetLeavesRemainingDirtyChecksQueued() {
        var first = Structures.define(ResourceLocation.parse("cumulus:first_engine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();
        var second = Structures.define(ResourceLocation.parse("cumulus:second_engine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();
        var engine = new RuntimeCheckEngine()
                .register(first)
                .register(second)
                .track(first.id(), new BlockPos(0, 64, 0), source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller"))
                .track(second.id(), new BlockPos(0, 64, 0), source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var firstFrame = engine.tick(20, 1);
        var secondFrame = engine.tick(21, 1);

        assertEquals(1, firstFrame.deltas().size());
        assertEquals(1, secondFrame.deltas().size());
        assertEquals(new StructureInstanceKey(first.id(), new BlockPos(0, 64, 0)), firstFrame.deltas().getFirst().key());
        assertEquals(new StructureInstanceKey(second.id(), new BlockPos(0, 64, 0)), secondFrame.deltas().getFirst().key());
    }

    @Test
    void missingSourceDataLeavesInstanceIncompleteAndEmitsRequests() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:missing_engine"))
                .point("controller", RelativeBlockPos.of(32, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();
        var engine = new RuntimeCheckEngine()
                .register(definition)
                .track(definition.id(), new BlockPos(0, 64, 0), source());

        engine.markDirty(new SectionKey(2, 4, 0));
        var frame = engine.tick(30, 4);

        assertEquals(InstanceStatus.INCOMPLETE, frame.deltas().getFirst().status());
        assertEquals(1, frame.requests().size());
        assertEquals(new SectionKey(2, 4, 0), frame.requests().getFirst().section());
    }

    @Test
    void substructurePointsAreComposedIntoRuntimeChecks() {
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child"))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port"))
                        .capture("port"))
                .build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0)))
                .build();
        var engine = new RuntimeCheckEngine()
                .register(parent)
                .register(child)
                .track(parent.id(), new BlockPos(0, 64, 0), source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                        .put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(40, 4);

        var parentDelta = frame.deltas().stream()
                .filter(delta -> delta.key().structureId().equals(parent.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(InstanceStatus.VALID, parentDelta.status());
        assertEquals("3,64,0", parentDelta.context().values()
                .get("cumulus:runtime_parent/left/port"));
    }

    @Test
    void substructureOccurrencesPublishIndependentChildDeltas() {
        var anchor = new BlockPos(0, 64, 0);
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_lifecycle"))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port"))
                        .capture("port"))
                .build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_lifecycle"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0)))
                .component(Components.substructure("right", child.id(), RelativeBlockPos.of(5, 0, 0)))
                .build();
        var engine = new RuntimeCheckEngine()
                .register(parent)
                .register(child)
                .track(parent.id(), anchor, source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                        .put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:port")
                        .put(BLOCK, RelativeBlockPos.of(6, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(45, 4);

        assertEquals(3, frame.deltas().size());
        var left = frame.deltas().stream()
                .filter(delta -> delta.key().structureId().equals(child.id()))
                .filter(delta -> delta.key().anchor().equals(anchor.offset(2, 0, 0)))
                .findFirst()
                .orElseThrow();
        var right = frame.deltas().stream()
                .filter(delta -> delta.key().structureId().equals(child.id()))
                .filter(delta -> delta.key().anchor().equals(anchor.offset(5, 0, 0)))
                .findFirst()
                .orElseThrow();
        assertEquals(InstanceStatus.VALID, left.status());
        assertEquals(InstanceStatus.VALID, right.status());
        var parentDelta = frame.deltas().stream()
                .filter(delta -> delta.key().structureId().equals(parent.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(InstanceStatus.VALID.name(), parentDelta.context().values()
                .get("substructure.cumulus:runtime_parent_lifecycle/left.status"));
        assertEquals(InstanceStatus.VALID.name(), parentDelta.context().values()
                .get("substructure.cumulus:runtime_parent_lifecycle/right.status"));
        assertEquals("cumulus:runtime_parent_lifecycle/left", left.context().values().get("parent_occurrence"));
        assertEquals("cumulus:runtime_parent_lifecycle/right", right.context().values().get("parent_occurrence"));
        assertEquals(parent.id().toString(), left.context().values().get("parent_structure"));
        assertEquals("0,64,0", left.context().values().get("parent_anchor"));
        assertEquals(InstanceStatus.VALID.name(), left.context().values().get("parent_status"));
        assertEquals("3,64,0", left.context().values().get("port"));
        assertEquals("6,64,0", right.context().values().get("port"));
    }

    @Test
    void substructureSyncPolicyMirrorsChildContextAndPublishesLifecycleCallbacks() {
        var anchor = new BlockPos(0, 64, 0);
        var events = new ArrayList<SubstructureLifecycleEvent>();
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_sync"))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port"))
                        .capture("port"))
                .build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_sync"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0)))
                .build();
        var source = source()
                .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:port");
        var engine = new RuntimeCheckEngine()
                .substructureSyncPolicy(SubstructureSyncPolicy.STATUS_AND_CONTEXT)
                .addSubstructureLifecycleListener(events::add)
                .register(parent)
                .register(child)
                .track(parent.id(), anchor, source);

        engine.markDirty(new SectionKey(0, 4, 0));
        var validFrame = engine.tick(47, 4);

        var occurrence = "cumulus:runtime_parent_sync/left";
        var parentDelta = validFrame.deltas().stream()
                .filter(delta -> delta.key().structureId().equals(parent.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("3,64,0", parentDelta.context().values()
                .get("substructure." + occurrence + ".context.port"));
        assertEquals("2,0,0", parentDelta.context().values()
                .get("substructure." + occurrence + ".anchor_offset"));
        assertEquals(1, events.size());
        assertEquals(SubstructureLifecycleTransition.ADDED, events.getFirst().transition());
        assertEquals(Optional.empty(), events.getFirst().previousStatus());
        assertEquals(InstanceStatus.VALID, events.getFirst().currentStatus());
        assertEquals(occurrence, events.getFirst().occurrenceId());

        source.put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:wrong_port");
        engine.markDirty(new SectionKey(0, 4, 0));
        var invalidFrame = engine.tick(48, 4);

        assertEquals(2, events.size());
        assertEquals(SubstructureLifecycleTransition.STATUS_CHANGED, events.get(1).transition());
        assertEquals(Optional.of(InstanceStatus.VALID), events.get(1).previousStatus());
        assertEquals(InstanceStatus.INVALID, events.get(1).currentStatus());
        var invalidParentDelta = invalidFrame.deltas().stream()
                .filter(delta -> delta.key().structureId().equals(parent.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(InstanceStatus.INVALID.name(), invalidParentDelta.context().values()
                .get("substructure." + occurrence + ".status"));
    }

    @Test
    void defaultSubstructureSyncPolicyPublishesStatusWithoutMirroringChildContext() {
        var anchor = new BlockPos(0, 64, 0);
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_default_sync"))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port"))
                        .capture("port"))
                .build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_default_sync"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0)))
                .build();
        var engine = new RuntimeCheckEngine()
                .register(parent)
                .register(child)
                .track(parent.id(), anchor, source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                        .put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(49, 4);

        var occurrence = "cumulus:runtime_parent_default_sync/left";
        var parentDelta = frame.deltas().stream()
                .filter(delta -> delta.key().structureId().equals(parent.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(InstanceStatus.VALID.name(), parentDelta.context().values()
                .get("substructure." + occurrence + ".status"));
        assertFalse(parentDelta.context().values()
                .containsKey("substructure." + occurrence + ".context.port"));
    }

    @Test
    void substructureOccurrenceDiagnosticsAreScopedToChildInstance() {
        var anchor = new BlockPos(0, 64, 0);
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_diagnostics"))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port")))
                .build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_diagnostics"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0)))
                .build();
        var engine = new RuntimeCheckEngine()
                .register(parent)
                .register(child)
                .track(parent.id(), anchor, source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                        .put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:wrong_port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(46, 4);

        var childKey = new StructureInstanceKey(child.id(), anchor.offset(2, 0, 0));
        var childDelta = frame.deltas().stream()
                .filter(delta -> delta.key().equals(childKey))
                .findFirst()
                .orElseThrow();
        var childDiagnostic = engine.diagnostics(childKey).childrenOf("root").getFirst();
        assertEquals(InstanceStatus.INVALID, childDelta.status());
        assertEquals("port", childDiagnostic.id());
        assertEquals(RelativeBlockPos.of(1, 0, 0), childDiagnostic.evidence().getFirst().relativePosition());
        assertEquals("port", childDiagnostic.evidence().getFirst().componentPath());
    }

    @Test
    void substructureChildOccurrencesDetectIndependentOwnershipConflicts() {
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_claims"))
                .point("port", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port")))
                .build();
        var firstParent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_claims_first"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("shared", child.id(), RelativeBlockPos.of(2, 0, 0)))
                .build();
        var secondParent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_claims_second"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("shared", child.id(), RelativeBlockPos.of(-2, 0, 0)))
                .build();
        var firstAnchor = new BlockPos(0, 64, 0);
        var secondAnchor = new BlockPos(4, 64, 0);
        var engine = new RuntimeCheckEngine()
                .register(firstParent)
                .register(secondParent)
                .register(child)
                .track(firstParent.id(), firstAnchor, source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                        .put(BLOCK, RelativeBlockPos.of(2, 0, 0), "cumulus:port"))
                .track(secondParent.id(), secondAnchor, source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                        .put(BLOCK, RelativeBlockPos.of(-2, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var first = engine.tick(80, 1);
        engine.markDirty(new SectionKey(0, 4, 0));
        var second = engine.tick(81, 1);

        var childKey = new StructureInstanceKey(child.id(), firstAnchor.offset(2, 0, 0));
        assertEquals(InstanceStatus.VALID, first.deltas().stream()
                .filter(delta -> delta.key().equals(childKey))
                .findFirst()
                .orElseThrow()
                .status());
        assertEquals(InstanceStatus.INVALID, second.deltas().stream()
                .filter(delta -> delta.key().equals(childKey))
                .findFirst()
                .orElseThrow()
                .status());
        var diagnostic = engine.diagnostics(childKey).childrenOf("root").getFirst();
        assertEquals("ownership-conflict", diagnostic.id());
        assertEquals("OWNERSHIP_CONFLICT", diagnostic.evidence().getFirst().detail());
    }

    @Test
    void substructureChildOwnershipDoesNotConflictWithSameParentOnRepeatedChecks() {
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_same_claim"))
                .point("port", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port")))
                .build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_same_claim"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("child", child.id(), RelativeBlockPos.of(2, 0, 0)))
                .build();
        var anchor = new BlockPos(0, 64, 0);
        var engine = new RuntimeCheckEngine()
                .register(parent)
                .register(child)
                .track(parent.id(), anchor, source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                        .put(BLOCK, RelativeBlockPos.of(2, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        engine.tick(82, 1);
        engine.markDirty(new SectionKey(0, 4, 0));
        var second = engine.tick(83, 1);

        var childKey = new StructureInstanceKey(child.id(), anchor.offset(2, 0, 0));
        assertEquals(InstanceStatus.VALID, second.deltas().stream()
                .filter(delta -> delta.key().equals(childKey))
                .findFirst()
                .orElseThrow()
                .status());
        assertTrue(engine.diagnostics(childKey).nodes().isEmpty());
    }

    @Test
    void unresolvedSubstructureDefinitionsInvalidateBeforeSnapshotReads() {
        var anchor = new BlockPos(0, 64, 0);
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_missing_child"))
                .component(Components.substructure(
                        "missing",
                        ResourceLocation.parse("cumulus:runtime_missing_child"),
                        RelativeBlockPos.of(1, 0, 0)))
                .build();
        var source = source();
        var engine = new RuntimeCheckEngine()
                .register(parent)
                .track(parent.id(), anchor, source);

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(50, 4);

        assertEquals(1, frame.deltas().size());
        assertEquals(InstanceStatus.INVALID, frame.deltas().getFirst().status());
        assertEquals(0, source.reads);
        var diagnostic = engine.diagnostics(new StructureInstanceKey(parent.id(), anchor))
                .childrenOf("root")
                .getFirst();
        assertEquals("MISSING_SUBSTRUCTURE_DEFINITION", diagnostic.evidence().getFirst().detail());
    }

    @Test
    void runtimeUsesCompiledCheckPlanForRepeatedChecks() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:compiled_runtime"))
                .transforms(dev.vfyjxf.cumulus.api.transform.Transforms.identity(),
                        dev.vfyjxf.cumulus.api.transform.Transforms.rotateY90())
                .point("controller", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller"))
                        .capture("controller"))
                .build();
        var engine = new RuntimeCheckEngine()
                .register(definition)
                .track(definition.id(), new BlockPos(0, 64, 0), source()
                        .put(BLOCK, RelativeBlockPos.of(0, 0, 1), "cumulus:controller"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var first = engine.tick(60, 4);
        engine.markDirty(new SectionKey(0, 4, 0));
        var second = engine.tick(61, 4);

        assertEquals(InstanceStatus.VALID, first.deltas().getFirst().status());
        assertEquals(InstanceStatus.VALID, second.deltas().getFirst().status());
        assertEquals("0,64,1", second.deltas().getFirst().context().values().get("controller"));
        assertEquals(1, engine.compiledCheckPlanCount());
    }

    @Test
    void fastTickUsesFastCheckPathAndSkipsDiagnosticDeltas() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:fast_runtime"))
                .transforms(
                        dev.vfyjxf.cumulus.api.transform.Transforms.identity(),
                        dev.vfyjxf.cumulus.api.transform.Transforms.rotateY90(),
                        dev.vfyjxf.cumulus.api.transform.Transforms.rotateY180())
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port")))
                .build();
        var source = source()
                .put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .put(BLOCK, RelativeBlockPos.of(0, 0, 1), "cumulus:port");
        var engine = new RuntimeCheckEngine()
                .register(definition)
                .track(definition.id(), new BlockPos(0, 64, 0), source);

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.fastTick(70, 4);

        assertEquals(List.of(), frame.deltas());
        assertEquals(List.of(), frame.requests());
        assertEquals(1, frame.fastChecks().size());
        var fastCheck = frame.fastChecks().getFirst();
        assertEquals(InstanceStatus.VALID, fastCheck.status());
        assertEquals(2, fastCheck.candidatesEvaluated());
        assertEquals(4, fastCheck.entriesEvaluated());
        assertEquals(1, fastCheck.validCandidateIndex());
        assertEquals(4, source.reads);
    }

    private static BuilderSource source() {
        return new BuilderSource();
    }

    private static final class BuilderSource implements TargetSnapshotSource {
        private final Map<Key, SnapshotTarget> targets = new LinkedHashMap<>();

        BuilderSource put(ResourceLocation targetType, RelativeBlockPos pos, String value) {
            targets.put(new Key(targetType, pos, pos), new SnapshotTarget(value, Map.of()));
            return this;
        }

        @Override
        public Optional<SnapshotTarget> target(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max) {
            reads++;
            return Optional.ofNullable(targets.get(new Key(targetType, min, max)));
        }

        @Override
        public Optional<String> context(String key) {
            return Optional.empty();
        }

        private record Key(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max) {
        }

        private int reads;
    }
}
