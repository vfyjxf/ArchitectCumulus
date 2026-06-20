package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;
import dev.vfyjxf.cumulus.impl.check.TargetSnapshotSource;
import dev.vfyjxf.cumulus.impl.codec.LegacySourceParser;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCheckEngineTest {
    private static final ResourceLocation BLOCK = ResourceLocation.parse("cumulus:block");
    private static final ResourceLocation REGION = ResourceLocation.parse("cumulus:region");

    @Test
    void dirtySectionsRunStructureChecksAndPublishDeltas() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:runtime_engine")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller")).capture("controller")).build();
        var engine = new RuntimeCheckEngine().register(definition).track(definition.id(), new BlockPos(0, 64, 0), source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(10, 4);

        assertEquals(1, frame.deltas().size());
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
        assertEquals("0,64,0", frame.deltas().getFirst().context().get(ContextKey.string("controller")).orElse(null));
        assertEquals(List.of(), frame.requests());
    }

    @Test
    void frameBudgetLeavesRemainingDirtyChecksQueued() {
        var first = Structures.define(ResourceLocation.parse("cumulus:first_engine")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).build();
        var second = Structures.define(ResourceLocation.parse("cumulus:second_engine")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).build();
        var engine = new RuntimeCheckEngine().register(first).register(second).track(first.id(), new BlockPos(0, 64, 0), source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller")).track(second.id(), new BlockPos(0, 64, 0), source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller"));

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
        var definition = Structures.define(ResourceLocation.parse("cumulus:missing_engine")).point("controller", RelativeBlockPos.of(32, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).build();
        var engine = new RuntimeCheckEngine().register(definition).track(definition.id(), new BlockPos(0, 64, 0), source());

        engine.markDirty(new SectionKey(2, 4, 0));
        var frame = engine.tick(30, 4);

        assertEquals(InstanceStatus.incomplete, frame.deltas().getFirst().status());
        assertEquals(1, frame.requests().size());
        assertEquals(new SectionKey(2, 4, 0), frame.requests().getFirst().section());
    }

    @Test
    void substructurePointsAreComposedIntoRuntimeChecks() {
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child")).point("port", RelativeBlockPos.of(1, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:port")).capture("port")).build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0))).build();
        var engine = new RuntimeCheckEngine().register(parent).register(child).track(parent.id(), new BlockPos(0, 64, 0), source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(40, 4);

        var parentDelta = frame.deltas().stream().filter(delta -> delta.key().structureId().equals(parent.id())).findFirst().orElseThrow();
        assertEquals(InstanceStatus.valid, parentDelta.status());
        assertEquals("3,64,0", parentDelta.context().get(ContextKey.string("cumulus:runtime_parent/left/port")).orElse(null));
    }

    @Test
    void substructureOccurrencesPublishIndependentChildDeltas() {
        var anchor = new BlockPos(0, 64, 0);
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_lifecycle")).point("port", RelativeBlockPos.of(1, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:port")).capture("port")).build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_lifecycle")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0))).component(Components.substructure("right", child.id(), RelativeBlockPos.of(5, 0, 0))).build();
        var engine = new RuntimeCheckEngine().register(parent).register(child).track(parent.id(), anchor, source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:port").put(BLOCK, RelativeBlockPos.of(6, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(45, 4);

        assertEquals(3, frame.deltas().size());
        var left = frame.deltas().stream().filter(delta -> delta.key().structureId().equals(child.id())).filter(delta -> delta.key().anchor().equals(anchor.offset(2, 0, 0))).findFirst().orElseThrow();
        var right = frame.deltas().stream().filter(delta -> delta.key().structureId().equals(child.id())).filter(delta -> delta.key().anchor().equals(anchor.offset(5, 0, 0))).findFirst().orElseThrow();
        assertEquals(InstanceStatus.valid, left.status());
        assertEquals(InstanceStatus.valid, right.status());
        var parentDelta = frame.deltas().stream().filter(delta -> delta.key().structureId().equals(parent.id())).findFirst().orElseThrow();
        assertEquals(InstanceStatus.valid.name(), parentDelta.context().get(ContextKey.string("substructure.cumulus:runtime_parent_lifecycle/left.status")).orElse(null));
        assertEquals(InstanceStatus.valid.name(), parentDelta.context().get(ContextKey.string("substructure.cumulus:runtime_parent_lifecycle/right.status")).orElse(null));
        assertEquals("cumulus:runtime_parent_lifecycle/left", left.context().get(ContextKey.string("parent_occurrence")).orElse(null));
        assertEquals("cumulus:runtime_parent_lifecycle/right", right.context().get(ContextKey.string("parent_occurrence")).orElse(null));
        assertEquals(parent.id().toString(), left.context().get(ContextKey.string("parent_structure")).orElse(null));
        assertEquals("0,64,0", left.context().get(ContextKey.string("parent_anchor")).orElse(null));
        assertEquals(InstanceStatus.valid.name(), left.context().get(ContextKey.string("parent_status")).orElse(null));
        assertEquals("3,64,0", left.context().get(ContextKey.string("port")).orElse(null));
        assertEquals("6,64,0", right.context().get(ContextKey.string("port")).orElse(null));
    }

    @Test
    void substructureSyncPolicyMirrorsChildContextAndPublishesLifecycleCallbacks() {
        var anchor = new BlockPos(0, 64, 0);
        var events = new ArrayList<SubstructureLifecycleEvent>();
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_sync")).point("port", RelativeBlockPos.of(1, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:port")).capture("port")).build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_sync")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0))).build();
        var source = source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:port");
        var engine = new RuntimeCheckEngine().substructureSyncPolicy(SubstructureSyncPolicy.statusAndContext).addSubstructureLifecycleListener(events::add).register(parent).register(child).track(parent.id(), anchor, source);

        engine.markDirty(new SectionKey(0, 4, 0));
        var validFrame = engine.tick(47, 4);

        var occurrence = "cumulus:runtime_parent_sync/left";
        var parentDelta = validFrame.deltas().stream().filter(delta -> delta.key().structureId().equals(parent.id())).findFirst().orElseThrow();
        assertEquals("3,64,0", parentDelta.context().get(ContextKey.string("substructure." + occurrence + ".context.port")).orElse(null));
        assertEquals("2,0,0", parentDelta.context().get(ContextKey.string("substructure." + occurrence + ".anchor_offset")).orElse(null));
        assertEquals(1, events.size());
        assertEquals(SubstructureLifecycleTransition.added, events.getFirst().transition());
        assertEquals(Optional.empty(), events.getFirst().previousStatus());
        assertEquals(InstanceStatus.valid, events.getFirst().currentStatus());
        assertEquals(occurrence, events.getFirst().occurrenceId());

        source.put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:wrong_port");
        engine.markDirty(new SectionKey(0, 4, 0));
        var invalidFrame = engine.tick(48, 4);

        assertEquals(2, events.size());
        assertEquals(SubstructureLifecycleTransition.statusChanged, events.get(1).transition());
        assertEquals(Optional.of(InstanceStatus.valid), events.get(1).previousStatus());
        assertEquals(InstanceStatus.invalid, events.get(1).currentStatus());
        var invalidParentDelta = invalidFrame.deltas().stream().filter(delta -> delta.key().structureId().equals(parent.id())).findFirst().orElseThrow();
        assertEquals(InstanceStatus.invalid.name(), invalidParentDelta.context().get(ContextKey.string("substructure." + occurrence + ".status")).orElse(null));
    }

    @Test
    void defaultSubstructureSyncPolicyPublishesStatusWithoutMirroringChildContext() {
        var anchor = new BlockPos(0, 64, 0);
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_default_sync")).point("port", RelativeBlockPos.of(1, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:port")).capture("port")).build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_default_sync")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0))).build();
        var engine = new RuntimeCheckEngine().register(parent).register(child).track(parent.id(), anchor, source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(49, 4);

        var occurrence = "cumulus:runtime_parent_default_sync/left";
        var parentDelta = frame.deltas().stream().filter(delta -> delta.key().structureId().equals(parent.id())).findFirst().orElseThrow();
        assertEquals(InstanceStatus.valid.name(), parentDelta.context().get(ContextKey.string("substructure." + occurrence + ".status")).orElse(null));
        assertFalse(parentDelta.context().get(ContextKey.string("substructure." + occurrence + ".context.port")).isPresent());
    }

    @Test
    void substructureOccurrenceDiagnosticsAreScopedToChildInstance() {
        var anchor = new BlockPos(0, 64, 0);
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_diagnostics")).point("port", RelativeBlockPos.of(1, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:port"))).build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_diagnostics")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.substructure("left", child.id(), RelativeBlockPos.of(2, 0, 0))).build();
        var engine = new RuntimeCheckEngine().register(parent).register(child).track(parent.id(), anchor, source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:wrong_port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(46, 4);

        var childKey = new StructureInstanceKey(child.id(), anchor.offset(2, 0, 0));
        var childDelta = frame.deltas().stream().filter(delta -> delta.key().equals(childKey)).findFirst().orElseThrow();
        var childDiagnostic = engine.diagnostics(childKey).childrenOf("root").getFirst();
        assertEquals(InstanceStatus.invalid, childDelta.status());
        assertEquals("port", childDiagnostic.id());
        assertEquals(RelativeBlockPos.of(1, 0, 0), childDiagnostic.evidence().getFirst().relativePosition());
        assertEquals("port", childDiagnostic.evidence().getFirst().componentPath());
    }

    @Test
    void substructureChildOccurrencesDetectIndependentOwnershipConflicts() {
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_claims")).point("port", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:port"))).build();
        var firstParent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_claims_first")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.substructure("shared", child.id(), RelativeBlockPos.of(2, 0, 0))).build();
        var secondParent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_claims_second")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.substructure("shared", child.id(), RelativeBlockPos.of(-2, 0, 0))).build();
        var firstAnchor = new BlockPos(0, 64, 0);
        var secondAnchor = new BlockPos(4, 64, 0);
        var engine = new RuntimeCheckEngine().register(firstParent).register(secondParent).register(child).track(firstParent.id(), firstAnchor, source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(2, 0, 0), "cumulus:port")).track(secondParent.id(), secondAnchor, source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(-2, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var first = engine.tick(80, 1);
        engine.markDirty(new SectionKey(0, 4, 0));
        var second = engine.tick(81, 1);

        var childKey = new StructureInstanceKey(child.id(), firstAnchor.offset(2, 0, 0));
        assertEquals(InstanceStatus.valid, first.deltas().stream().filter(delta -> delta.key().equals(childKey)).findFirst().orElseThrow().status());
        assertEquals(InstanceStatus.invalid, second.deltas().stream().filter(delta -> delta.key().equals(childKey)).findFirst().orElseThrow().status());
        var diagnostic = engine.diagnostics(childKey).childrenOf("root").getFirst();
        assertEquals("ownership-conflict", diagnostic.id());
        assertEquals("OWNERSHIP_CONFLICT", diagnostic.evidence().getFirst().detail());
    }

    @Test
    void substructureChildOwnershipDoesNotConflictWithSameParentOnRepeatedChecks() {
        var child = Structures.define(ResourceLocation.parse("cumulus:runtime_child_same_claim")).point("port", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:port"))).build();
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_same_claim")).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.substructure("child", child.id(), RelativeBlockPos.of(2, 0, 0))).build();
        var anchor = new BlockPos(0, 64, 0);
        var engine = new RuntimeCheckEngine().register(parent).register(child).track(parent.id(), anchor, source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(2, 0, 0), "cumulus:port"));

        engine.markDirty(new SectionKey(0, 4, 0));
        engine.tick(82, 1);
        engine.markDirty(new SectionKey(0, 4, 0));
        var second = engine.tick(83, 1);

        var childKey = new StructureInstanceKey(child.id(), anchor.offset(2, 0, 0));
        assertEquals(InstanceStatus.valid, second.deltas().stream().filter(delta -> delta.key().equals(childKey)).findFirst().orElseThrow().status());
        assertTrue(engine.diagnostics(childKey).nodes().isEmpty());
    }

    @Test
    void complexNestedSubstructureCaseConvertsStructureDataIntoRuntimeContexts() {
        var base = Structures.define(ResourceLocation.parse("cumulus:runtime_base")).point("base_controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller")).capture("base_controller")).context(ContextRuleDescriptor.derive(ContextKey.string("base_facing"), LegacySourceParser.parse("base_controller.facing"))).build();
        var arm = Structures.define(ResourceLocation.parse("cumulus:runtime_arm_complex")).point("arm_mount", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:arm_mount")).capture("arm_mount")).component(Components.substructure("tool", ResourceLocation.parse("cumulus:runtime_tool_complex"), RelativeBlockPos.of(1, 0, 0))).context(ContextRuleDescriptor.derive(ContextKey.string("arm_mount_value"), LegacySourceParser.parse("arm_mount.value"))).build();
        var tool = Structures.define(ResourceLocation.parse("cumulus:runtime_tool_complex")).point("tool_head", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:tool_head")).capture("tool_head")).point("tool_region", RelativeBlockPos.of(-1, 0, 0), Targets.region(RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 0, 0)).where(PredicateDescriptors.id("cumulus:tool_region"))).context(ContextRuleDescriptor.derive(ContextKey.string("tool_head_value"), LegacySourceParser.parse("tool_head.value"))).build();
        var factory = Structures.define(ResourceLocation.parse("cumulus:runtime_factory_complex")).point("factory_core", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:factory_core")).capture("factory_core")).component(Components.substructure("left_arm", arm.id(), RelativeBlockPos.of(2, 0, 0))).component(Components.substructure("right_arm", arm.id(), RelativeBlockPos.of(-2, 0, 0))).component(Components.substructure("base", base.id(), RelativeBlockPos.of(0, -1, 0))).context(ContextRuleDescriptor.derive(ContextKey.string("factory_core_value"), LegacySourceParser.parse("factory_core.value"))).build();

        var engine = new RuntimeCheckEngine().substructureSyncPolicy(SubstructureSyncPolicy.statusAndContext).register(factory).register(arm).register(tool).register(base).track(factory.id(), new BlockPos(100, 64,
                        100), source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:factory_core").put(BLOCK, RelativeBlockPos.of(2, 0, 0), "cumulus:arm_mount").put(BLOCK, RelativeBlockPos.of(3, 0, 0), "cumulus:tool_head").put(REGION, RelativeBlockPos.of(2, 0, 0), RelativeBlockPos.of(4, 0, 0), "cumulus:tool_region").put(BLOCK, RelativeBlockPos.of(-2, 0, 0), "cumulus:arm_mount").put(BLOCK, RelativeBlockPos.of(-1, 0, 0), "cumulus:tool_head").put(REGION, RelativeBlockPos.of(-2, 0, 0), RelativeBlockPos.of(0, 0, 0), "cumulus:tool_region").put(BLOCK, RelativeBlockPos.of(0, -1, 0), "cumulus:controller", Map.of("facing", "north")));

        engine.markDirty(new SectionKey(6, 4, 6));
        var frame = engine.tick(90, 8);

        var rootDelta = frame.deltas().stream().filter(delta -> delta.key().structureId().equals(factory.id())).findFirst().orElseThrow();
        assertEquals("cumulus:factory_core", rootDelta.context().get(ContextKey.string("factory_core_value")).orElse(null));
        assertEquals(InstanceStatus.valid.name(), rootDelta.context().get(ContextKey.string("substructure.cumulus:runtime_factory_complex/left_arm.status")).orElse(null));
        assertEquals("north", rootDelta.context().get(ContextKey.string("substructure.cumulus:runtime_factory_complex/base.context.base_facing")).orElse(null));

        var leftArmDelta = frame.deltas().stream().filter(delta -> delta.key().structureId().equals(arm.id())).filter(delta -> delta.key().anchor().equals(new BlockPos(102, 64, 100))).findFirst().orElseThrow();
        assertEquals("cumulus:arm_mount", leftArmDelta.context().get(ContextKey.string("arm_mount_value")).orElse(null));
        assertEquals("cumulus:runtime_factory_complex/left_arm", leftArmDelta.context().get(ContextKey.string("parent_occurrence")).orElse(null));

        var toolDelta = frame.deltas().stream().filter(delta -> delta.key().structureId().equals(tool.id())).filter(delta -> delta.key().anchor().equals(new BlockPos(103, 64, 100))).findFirst().orElseThrow();
        assertEquals("cumulus:tool_head", toolDelta.context().get(ContextKey.string("tool_head_value")).orElse(null));
        assertEquals("cumulus:runtime_factory_complex/left_arm/tool", toolDelta.context().get(ContextKey.string("parent_occurrence")).orElse(null));
        assertEquals("103,64,100", toolDelta.context().get(ContextKey.string("tool_head")).orElse(null));
    }

    @Test
    void unresolvedSubstructureDefinitionsInvalidateBeforeSnapshotReads() {
        var anchor = new BlockPos(0, 64, 0);
        var parent = Structures.define(ResourceLocation.parse("cumulus:runtime_parent_missing_child")).component(Components.substructure("missing", ResourceLocation.parse("cumulus:runtime_missing_child"), RelativeBlockPos.of(1, 0, 0))).build();
        var source = source();
        var engine = new RuntimeCheckEngine().register(parent).track(parent.id(), anchor, source);

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(50, 4);

        assertEquals(1, frame.deltas().size());
        assertEquals(InstanceStatus.invalid, frame.deltas().getFirst().status());
        assertEquals(0, source.reads);
        var diagnostic = engine.diagnostics(new StructureInstanceKey(parent.id(), anchor)).childrenOf("root").getFirst();
        assertEquals("MISSING_SUBSTRUCTURE_DEFINITION", diagnostic.evidence().getFirst().detail());
    }

    @Test
    void runtimeUsesCompiledCheckPlanForRepeatedChecks() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:compiled_runtime")).transforms(Transforms.identity(), Transforms.rotateY90()).point("controller", RelativeBlockPos.of(1, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller")).capture("controller")).build();
        var engine = new RuntimeCheckEngine().register(definition).track(definition.id(), new BlockPos(0, 64, 0), source().put(BLOCK, RelativeBlockPos.of(0, 0, 1), "cumulus:controller"));

        engine.markDirty(new SectionKey(0, 4, 0));
        var first = engine.tick(60, 4);
        engine.markDirty(new SectionKey(0, 4, 0));
        var second = engine.tick(61, 4);

        assertEquals(InstanceStatus.valid, first.deltas().getFirst().status());
        assertEquals(InstanceStatus.valid, second.deltas().getFirst().status());
        assertEquals("0,64,1", second.deltas().getFirst().context().get(ContextKey.string("controller")).orElse(null));
        assertEquals(1, engine.compiledCheckPlanCount());
    }

    @Test
    void fastTickUsesFastCheckPathAndSkipsDiagnosticDeltas() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:fast_runtime")).transforms(Transforms.identity(), Transforms.rotateY90(), Transforms.rotateY180()).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).point("port", RelativeBlockPos.of(1, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:port"))).build();
        var source = source().put(BLOCK, RelativeBlockPos.of(0, 0, 0), "cumulus:controller").put(BLOCK, RelativeBlockPos.of(0, 0, 1), "cumulus:port");
        var engine = new RuntimeCheckEngine().register(definition).track(definition.id(), new BlockPos(0, 64, 0), source);

        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.fastTick(70, 4);

        assertEquals(List.of(), frame.deltas());
        assertEquals(List.of(), frame.requests());
        assertEquals(1, frame.fastChecks().size());
        var fastCheck = frame.fastChecks().getFirst();
        assertEquals(InstanceStatus.valid, fastCheck.status());
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
            return put(targetType, pos, value, Map.of());
        }

        BuilderSource put(ResourceLocation targetType, RelativeBlockPos pos, String value, Map<String, String> properties) {
            return put(targetType, pos, pos, value, properties);
        }

        BuilderSource put(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max, String value) {
            return put(targetType, min, max, value, Map.of());
        }

        BuilderSource put(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max, String value, Map<String, String> properties) {
            targets.put(new Key(targetType, min, max), new SnapshotTarget(value, properties));
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
