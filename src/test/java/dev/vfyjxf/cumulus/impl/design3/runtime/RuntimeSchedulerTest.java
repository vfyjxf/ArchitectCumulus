package dev.vfyjxf.cumulus.impl.design3.runtime;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import dev.vfyjxf.cumulus.api.request.RequestReason;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;
import dev.vfyjxf.cumulus.impl.design3.RuntimePlanEntry;
import dev.vfyjxf.cumulus.impl.design3.StructureRuntimePlan;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeSchedulerTest {
    @Test
    void markingDirtySectionSchedulesOnlyInstancesWhoseFootprintContainsSection() {
        var scheduler = new RuntimeScheduler();
        var checked = new ArrayList<StructureInstanceKey>();
        var matching = key("cumulus:matching", new BlockPos(0, 64, 0));
        var outside = key("cumulus:outside", new BlockPos(32, 64, 0));
        scheduler.register(matching, plan("cumulus:matching", 0, 0));
        scheduler.register(outside, plan("cumulus:outside", 0, 0));

        scheduler.markDirty(new SectionKey(0, 4, 0));
        scheduler.drainDue(0, 10, recordingCheck(checked));

        assertEquals(List.of(matching), checked);
    }

    @Test
    void frameBudgetLimitsExecutedChecksAndLeavesRemainderForLaterFrame() {
        var scheduler = new RuntimeScheduler();
        var checked = new ArrayList<StructureInstanceKey>();
        var first = key("cumulus:first", new BlockPos(0, 64, 0));
        var second = key("cumulus:second", new BlockPos(0, 64, 0));
        scheduler.register(first, plan("cumulus:first", 0, 0));
        scheduler.register(second, plan("cumulus:second", 0, 0));

        scheduler.markDirty(new SectionKey(0, 4, 0));
        scheduler.drainDue(5, 1, recordingCheck(checked));
        assertEquals(List.of(first), checked);
        scheduler.drainDue(6, 1, recordingCheck(checked));

        assertEquals(List.of(first, second), checked);
    }

    @Test
    void repeatedDirtyMarksDoNotDuplicateSamePendingInstance() {
        var scheduler = new RuntimeScheduler();
        var checked = new ArrayList<StructureInstanceKey>();
        var instance = key("cumulus:machine", new BlockPos(0, 64, 0));
        scheduler.register(instance, plan("cumulus:machine", 0, 0));

        scheduler.markDirty(new SectionKey(0, 4, 0));
        scheduler.markDirty(new SectionKey(0, 4, 0));
        scheduler.drainDue(0, 10, recordingCheck(checked));

        assertEquals(List.of(instance), checked);
    }

    @Test
    void requestsFromCheckExecutionAreRecordedInLedgerInsteadOfDropped() {
        var scheduler = new RuntimeScheduler();
        var instance = key("cumulus:machine", new BlockPos(0, 64, 0));
        var request = new DataRequest(
                new SectionKey(1, 4, 0),
                RequestReason.CHECK_NEEDS_CONTEXT,
                RequestPriority.HIGH,
                20,
                "cumulus:machine/controller");
        scheduler.register(instance, plan("cumulus:machine", 0, 0));

        scheduler.markDirty(new SectionKey(0, 4, 0));
        var frame = scheduler.drainDue(3, 10, ignored -> new ScheduledCheckResult(List.of(), List.of(request)));
        scheduler.markDirty(new SectionKey(0, 4, 0));
        var duplicateFrame = scheduler.drainDue(4, 10, ignored -> new ScheduledCheckResult(List.of(), List.of(request)));

        assertEquals(List.of(request), frame.requests());
        assertEquals(List.of(), duplicateFrame.requests());
    }

    @Test
    void drainDuePublishesCheckDeltasOnEngineFrame() {
        var scheduler = new RuntimeScheduler();
        var instance = key("cumulus:machine", new BlockPos(0, 64, 0));
        var context = RuntimeStructureContext.builder(instance)
                .revision(7)
                .put("controller", "0,64,0")
                .build();
        var delta = new InstanceDelta(instance, InstanceStatus.VALID, context);
        scheduler.register(instance, plan("cumulus:machine", 0, 0));

        scheduler.markDirty(new SectionKey(0, 4, 0));
        var frame = scheduler.drainDue(9, 10, ignored -> new ScheduledCheckResult(List.of(delta), List.of()));

        assertEquals(List.of(delta), frame.deltas());
    }

    @Test
    void unregisteringInstanceRemovesQueuedWorkForIt() {
        var scheduler = new RuntimeScheduler();
        var checked = new ArrayList<StructureInstanceKey>();
        var first = key("cumulus:first", new BlockPos(0, 64, 0));
        var second = key("cumulus:second", new BlockPos(0, 64, 0));
        scheduler.register(first, plan("cumulus:first", 0, 0));
        scheduler.register(second, plan("cumulus:second", 0, 0));
        scheduler.markDirty(new SectionKey(0, 4, 0));

        scheduler.unregister(first);
        scheduler.drainDue(0, 10, recordingCheck(checked));

        assertEquals(List.of(second), checked);
    }

    private static ScheduledCheck recordingCheck(List<StructureInstanceKey> checked) {
        return instance -> {
            checked.add(instance);
            return ScheduledCheckResult.empty();
        };
    }

    private static StructureInstanceKey key(String id, BlockPos anchor) {
        return new StructureInstanceKey(ResourceLocation.parse(id), anchor);
    }

    private static StructureRuntimePlan plan(String id, int minX, int maxX) {
        return new StructureRuntimePlan(
                ResourceLocation.parse(id),
                List.of(new RuntimePlanEntry(
                        "body",
                        ResourceLocation.parse("cumulus:block"),
                        RelativeBlockPos.of(minX, 0, 0),
                        RelativeBlockPos.of(maxX, 0, 0))),
                List.of(),
                List.of(),
                new VisualizationModel(id + "/visualization", List.of()));
    }
}
