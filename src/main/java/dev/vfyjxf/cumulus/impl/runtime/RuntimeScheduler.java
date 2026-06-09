package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestLedger;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;

public final class RuntimeScheduler {
    private final Map<StructureInstanceKey, RegisteredInstance> instances = new LinkedHashMap<>();
    private final Map<SectionKey, Set<StructureInstanceKey>> instancesBySection = new LinkedHashMap<>();
    private final Set<StructureInstanceKey> queued = new LinkedHashSet<>();
    private final ArrayDeque<StructureInstanceKey> queue = new ArrayDeque<>();
    private final List<StructureInstanceKey> completedChecks = new ArrayList<>();
    private final RequestLedger requestLedger = new RequestLedger();

    public void register(StructureInstanceKey instance, StructureRuntimePlan plan) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance key must not be null");
        }
        if (plan == null) {
            throw new IllegalArgumentException("Runtime plan must not be null");
        }
        unregister(instance);
        var sections = footprintSections(instance.anchor(), plan);
        instances.put(instance, new RegisteredInstance(plan, sections));
        for (var section : sections) {
            instancesBySection.computeIfAbsent(section, ignored -> new LinkedHashSet<>()).add(instance);
        }
    }

    public void unregister(StructureInstanceKey instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance key must not be null");
        }
        var removed = instances.remove(instance);
        if (removed == null) {
            return;
        }
        for (var section : removed.sections()) {
            var sectionInstances = instancesBySection.get(section);
            if (sectionInstances == null) {
                continue;
            }
            sectionInstances.remove(instance);
            if (sectionInstances.isEmpty()) {
                instancesBySection.remove(section);
            }
        }
        queued.remove(instance);
        queue.remove(instance);
    }

    public void markDirty(SectionKey section) {
        if (section == null) {
            throw new IllegalArgumentException("Dirty section must not be null");
        }
        var affected = instancesBySection.get(section);
        if (affected == null) {
            return;
        }
        affected.stream()
                .sorted(Comparator
                        .comparing((StructureInstanceKey key) -> key.structureId().toString())
                        .thenComparingInt(key -> key.anchor().getX())
                        .thenComparingInt(key -> key.anchor().getY())
                        .thenComparingInt(key -> key.anchor().getZ()))
                .forEach(this::enqueueRegistered);
    }

    public void enqueue(StructureInstanceKey instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance key must not be null");
        }
        if (!instances.containsKey(instance)) {
            throw new IllegalArgumentException("No runtime instance registered for " + instance);
        }
        enqueueRegistered(instance);
    }

    public RuntimeFrameResult drainDue(int tick, int maxChecks, ScheduledCheck checker) {
        if (maxChecks < 0) {
            throw new IllegalArgumentException("Max checks must not be negative");
        }
        if (checker == null) {
            throw new IllegalArgumentException("Scheduled checker must not be null");
        }
        var checked = new ArrayList<StructureInstanceKey>();
        var deltas = new ArrayList<InstanceDelta>();
        var requests = new ArrayList<DataRequest>();
        var fastChecks = new ArrayList<RuntimeFastCheck>();
        while (!queue.isEmpty() && checked.size() < maxChecks) {
            var instance = queue.removeFirst();
            queued.remove(instance);
            if (!instances.containsKey(instance)) {
                continue;
            }
            var result = checker.check(instance);
            checked.add(instance);
            completedChecks.add(instance);
            deltas.addAll(result.deltas());
            requests.addAll(requestLedger.record(tick, result.requests().size(), result.requests().toArray(DataRequest[]::new)));
            fastChecks.addAll(result.fastChecks());
        }
        return new RuntimeFrameResult(tick, checked, deltas, requests, queue.size(), fastChecks);
    }

    public List<StructureInstanceKey> completedChecks() {
        return List.copyOf(completedChecks);
    }

    public int pendingChecks() {
        return queue.size();
    }

    private void enqueueRegistered(StructureInstanceKey instance) {
        if (queued.add(instance)) {
            queue.addLast(instance);
        }
    }

    private static Set<SectionKey> footprintSections(BlockPos anchor, StructureRuntimePlan plan) {
        var sections = new LinkedHashSet<SectionKey>();
        for (var entry : plan.entries()) {
            addSections(sections, anchor, entry.min(), entry.max());
        }
        return sections;
    }

    private static void addSections(
            Set<SectionKey> sections,
            BlockPos anchor,
            RelativeBlockPos min,
            RelativeBlockPos max) {
        var minSection = SectionKey.fromBlock(anchor.offset(min.x(), min.y(), min.z()));
        var maxSection = SectionKey.fromBlock(anchor.offset(max.x(), max.y(), max.z()));
        var minX = Math.min(minSection.x(), maxSection.x());
        var maxX = Math.max(minSection.x(), maxSection.x());
        var minY = Math.min(minSection.y(), maxSection.y());
        var maxY = Math.max(minSection.y(), maxSection.y());
        var minZ = Math.min(minSection.z(), maxSection.z());
        var maxZ = Math.max(minSection.z(), maxSection.z());
        for (var x = minX; x <= maxX; x++) {
            for (var y = minY; y <= maxY; y++) {
                for (var z = minZ; z <= maxZ; z++) {
                    sections.add(new SectionKey(x, y, z));
                }
            }
        }
    }

    private record RegisteredInstance(
            StructureRuntimePlan plan,
            Set<SectionKey> sections) {
        private RegisteredInstance {
            sections = Set.copyOf(sections);
        }
    }
}
