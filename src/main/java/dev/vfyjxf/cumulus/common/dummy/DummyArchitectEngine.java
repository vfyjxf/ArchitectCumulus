package dev.vfyjxf.cumulus.common.dummy;

import dev.vfyjxf.cumulus.api.ArchitectSchema;
import dev.vfyjxf.cumulus.api.binding.BindingsView;
import dev.vfyjxf.cumulus.api.context.BindContext;
import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.context.CompileContext;
import dev.vfyjxf.cumulus.api.context.DepContext;
import dev.vfyjxf.cumulus.api.context.ExportContext;
import dev.vfyjxf.cumulus.api.context.LinkContext;
import dev.vfyjxf.cumulus.api.context.PassContext;
import dev.vfyjxf.cumulus.api.context.PassSink;
import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.driver.DriverContext;
import dev.vfyjxf.cumulus.api.driver.DriverOutput;
import dev.vfyjxf.cumulus.api.driver.DriverStrategy;
import dev.vfyjxf.cumulus.api.engine.ArchitectEngine;
import dev.vfyjxf.cumulus.api.engine.Budget;
import dev.vfyjxf.cumulus.api.engine.ChangeCause;
import dev.vfyjxf.cumulus.api.engine.ChangeSet;
import dev.vfyjxf.cumulus.api.engine.EngineInstanceDirectory;
import dev.vfyjxf.cumulus.api.engine.EngineFrame;
import dev.vfyjxf.cumulus.api.engine.InstanceDelta;
import dev.vfyjxf.cumulus.api.engine.InstanceDirectory;
import dev.vfyjxf.cumulus.api.engine.InstanceKey;
import dev.vfyjxf.cumulus.api.engine.InstanceSnapshot;
import dev.vfyjxf.cumulus.api.engine.InstanceView;
import dev.vfyjxf.cumulus.api.engine.TickContext;
import dev.vfyjxf.cumulus.api.export.ExportView;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.interest.InterestView;
import dev.vfyjxf.cumulus.api.partition.LoadState;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.pass.BindReport;
import dev.vfyjxf.cumulus.api.pass.CheckReport;
import dev.vfyjxf.cumulus.api.pass.CompileReport;
import dev.vfyjxf.cumulus.api.pass.DependencyEdge;
import dev.vfyjxf.cumulus.api.pass.DependencyReport;
import dev.vfyjxf.cumulus.api.pass.ExportReport;
import dev.vfyjxf.cumulus.api.pass.LinkReport;
import dev.vfyjxf.cumulus.api.pass.ProbeResult;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 一个用于演示/测试的引擎实现（common/dummy）。
 *
 * <p>目标：把 v1 的 API/流程跑通，并提供可读性强的参考实现。
 * <p>非目标：生产级性能与完整特性（例如请求 ledger 去重/节流、并行执行、持久化缓存等）。
 *
 * <p>实现要点：
 * <ul>
 *     <li>以 {@link SectionKey} 作为 dirty 粒度</li>
 *     <li>支持外部 {@link ChangeSet} 提示 + 内置 {@link DriverStrategy} 主动扫描</li>
 *     <li>维护 interestIndex（section -> instances）以便 dirtySections 快速定位受影响实例</li>
 *     <li>支持 anchor discover/probe，并在 anchor 卸载时 evict 实例</li>
 *     <li>执行 bind/check/compile/dep/link/export 流水线，并汇总 requests/diagnostics/interest</li>
 *     <li>维护依赖边并检测环（cycle），对环上的实例追加诊断</li>
 * </ul>
 */
public final class DummyArchitectEngine implements ArchitectEngine {
    private static final ResourceLocation DIAG_DEP_CYCLE = ResourceLocation.fromNamespaceAndPath("cumulus", "dependency_cycle");

    private final PartitionAccess world;
    private final Map<ResourceLocation, ArchitectSchema> schemas = new HashMap<>();
    private final Deque<ChangeSet> pendingChanges = new ArrayDeque<>();
    private final List<DriverStrategy> drivers = new ArrayList<>();

    private final Map<InstanceKey, InstanceState> instances = new HashMap<>();
    private final Map<SectionKey, Set<InstanceKey>> interestIndex = new HashMap<>();
    private final Map<InstanceKey, List<DependencyEdge>> depEdgesByInstance = new HashMap<>();

    public DummyArchitectEngine(PartitionAccess world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    public DummyArchitectEngine addDriver(DriverStrategy driver) {
        drivers.add(Objects.requireNonNull(driver, "driver"));
        return this;
    }

    @Override
    public void register(ArchitectSchema schema) {
        schemas.put(schema.id(), schema);
    }

    @Override
    public void submit(ChangeSet changeSet) {
        pendingChanges.add(Objects.requireNonNull(changeSet, "changeSet"));
    }

    @Override
    public EngineFrame tick(TickContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        FrameBuilder frame = new FrameBuilder();

        Set<SectionKey> dirtySections = new HashSet<>();
        Set<BlockPos> dirtyAnchors = new HashSet<>();

        // 1) 合并外部提交的 changes（事件流）。
        while (!pendingChanges.isEmpty()) {
            ChangeSet changeSet = pendingChanges.pollFirst();
            dirtySections.addAll(changeSet.dirtySections());
            dirtyAnchors.addAll(changeSet.dirtyAnchors());
            frame.requests.addAll(changeSetToRequests(changeSet));
        }

        // 2) 运行 drivers（主动扫描）：补齐无事件流时的收敛能力。
        if (!drivers.isEmpty()) {
            DriverContext driverCtx = new DriverContextImpl(ctx, world, new InstanceDirectoryView(instances));
            for (DriverStrategy driver : drivers) {
                DriverOutput out = driver.run(driverCtx);
                dirtySections.addAll(out.dirtySections());
                dirtyAnchors.addAll(out.dirtyAnchors());
                frame.requests.addAll(out.requests());
            }
        }

        Set<InstanceKey> affected = new HashSet<>();
        Set<InstanceKey> addedThisTick = new HashSet<>();
        // 3) 驱逐：anchor 所在 section 卸载时，移除实例（v1 选择 evict 策略）。
        evictUnloaded(frame);
        // 4) 根据 dirtySections 发现新增 anchor（仅已加载范围内）。
        discoverAnchors(ctx, dirtySections, affected, addedThisTick);
        // 5) 根据 dirtySections/dirtyAnchors/interestIndex 定位受影响实例。
        markAffectedByDirty(dirtySections, dirtyAnchors, affected);
        // 6) 对既有实例进行 probe：anchor 不再存在则移除。
        probeAffected(ctx, affected, addedThisTick, frame);

        // 7) 对受影响实例执行流水线并输出 deltas。
        for (InstanceKey key : affected) {
            ArchitectSchema schema = schemas.get(key.schemaId());
            if (schema == null) {
                continue;
            }
            InstanceState prev = instances.get(key);
            InstanceState next = runPipeline(ctx, schema, key, prev, frame);
            instances.put(key, next);
            frame.deltas.add(addedThisTick.contains(key) ? InstanceDelta.added(next.snapshot()) : InstanceDelta.updated(next.snapshot()));
        }

        // 8) 依赖环检测：把 cycle 诊断追加到相关实例（示例实现：检测到任意环就标记环上节点）。
        applyCycleDiagnostics(frame);

        return frame.build();
    }

    @Override
    public Optional<InstanceView> instance(InstanceKey key) {
        InstanceState state = instances.get(Objects.requireNonNull(key, "key"));
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(state.snapshot());
    }

    private void evictUnloaded(FrameBuilder frame) {
        List<InstanceKey> toRemove = new ArrayList<>();
        for (Map.Entry<InstanceKey, InstanceState> entry : instances.entrySet()) {
            InstanceKey key = entry.getKey();
            SectionKey anchorSection = SectionKey.from(key.anchorPos());
            if (world.loadState(anchorSection) == LoadState.MISSING) {
                toRemove.add(key);
            }
        }
        for (InstanceKey key : toRemove) {
            InstanceState removed = instances.remove(key);
            if (removed != null) {
                depEdgesByInstance.remove(key);
                removeInterestIndex(key, removed.interest());
                frame.deltas.add(InstanceDelta.removed(key, Optional.of(removed.snapshot())));
            }
        }
    }

    private void discoverAnchors(TickContext ctx, Set<SectionKey> dirtySections, Set<InstanceKey> affected, Set<InstanceKey> addedThisTick) {
        if (dirtySections.isEmpty() || schemas.isEmpty()) {
            return;
        }
        Budget budget = ctx.budget();
        // 对每个 schema 在 dirty section 内 discover anchors。
        for (ArchitectSchema schema : schemas.values()) {
            AnchorFinder finder = schema.anchorFinder();
            for (SectionKey section : dirtySections) {
                if (world.loadState(section) != LoadState.LOADED) {
                    continue;
                }
                if (!budget.tryConsume(1)) {
                    return;
                }
                try (Stream<BlockPos> stream = finder.discover(new dev.vfyjxf.cumulus.api.context.DiscoverContext(world, section, budget))) {
                    stream.forEach(anchorPos -> {
                        InstanceKey key = new InstanceKey(schema.id(), anchorPos);
                        if (!instances.containsKey(key)) {
                            instances.put(key, InstanceState.empty(key));
                            affected.add(key);
                            addedThisTick.add(key);
                        }
                    });
                }
            }
        }
    }

    private void markAffectedByDirty(Set<SectionKey> dirtySections, Set<BlockPos> dirtyAnchors, Set<InstanceKey> affected) {
        // dirtyAnchors：直接点名重算。
        for (BlockPos anchorPos : dirtyAnchors) {
            for (ArchitectSchema schema : schemas.values()) {
                InstanceKey key = new InstanceKey(schema.id(), anchorPos);
                if (instances.containsKey(key)) {
                    affected.add(key);
                }
            }
        }
        // dirtySections：通过 interestIndex 快速找到受影响实例。
        for (SectionKey section : dirtySections) {
            Set<InstanceKey> keys = interestIndex.get(section);
            if (keys != null) {
                affected.addAll(keys);
            }
        }
        // anchor 本身所在 section dirty 时也应重算（即使没声明 interest）。
        for (InstanceKey key : instances.keySet()) {
            if (dirtySections.contains(SectionKey.from(key.anchorPos()))) {
                affected.add(key);
            }
        }
    }

    private void probeAffected(TickContext ctx, Set<InstanceKey> affected, Set<InstanceKey> addedThisTick, FrameBuilder frame) {
        if (affected.isEmpty()) {
            return;
        }
        Budget budget = ctx.budget();
        Set<InstanceKey> toRemove = new HashSet<>();
        for (InstanceKey key : affected) {
            if (addedThisTick.contains(key)) {
                continue;
            }
            ArchitectSchema schema = schemas.get(key.schemaId());
            if (schema == null) {
                continue;
            }
            if (!budget.tryConsume(1)) {
                break;
            }
            ProbeResult probe = schema.anchorFinder().probe(key.anchorPos(), new dev.vfyjxf.cumulus.api.context.ProbeContext(world, budget));
            if (probe != ProbeResult.PRESENT) {
                toRemove.add(key);
            }
        }
        for (InstanceKey key : toRemove) {
            InstanceState removed = instances.remove(key);
            if (removed != null) {
                depEdgesByInstance.remove(key);
                removeInterestIndex(key, removed.interest());
                frame.deltas.add(InstanceDelta.removed(key, Optional.of(removed.snapshot())));
            }
            affected.remove(key);
        }
    }

    private InstanceState runPipeline(TickContext tick, ArchitectSchema schema, InstanceKey key, @SuppressWarnings("unused") InstanceState prev, FrameBuilder frame) {
        CollectingPassSink sink = new CollectingPassSink();
        InstanceDirectory directory = new InstanceDirectoryView(instances);

        // 1) Bind：生成 bindings，并可能提出缺数据请求/兴趣集更新。
        BindReport bind = schema.binder().bind(new BindContextImpl(key, world, directory, tick.budget(), sink));
        sink.merge(bind);
        frame.requests.addAll(sink.requests());

        InterestView interest = applyInterest(InterestView.empty(), sink.interestUpdate());

        if (bind.status() != InstanceStatus.OK || bind.bindings().isEmpty()) {
            InstanceSnapshot snapshot = new InstanceSnapshot(
                    key,
                    bind.status(),
                    sink.diagnostics(),
                    Optional.empty(),
                    Optional.empty(),
                    interest
            );
            return new InstanceState(snapshot, bind.bindings(), Optional.empty(), Optional.empty(), interest);
        }

        BindingsView bindings = bind.bindings().get();

        InstanceStatus status = InstanceStatus.OK;
        // 2) Checkers：按声明顺序执行。
        for (ArchitectSchema.NamedChecker entry : schema.checkers()) {
            CheckReport check = entry.checker().check(new CheckContextImpl(key, world, directory, tick.budget(), sink, bindings));
            sink.merge(check);
            status = worse(status, check.status());
        }

        interest = applyInterest(interest, sink.interestUpdate());
        frame.requests.addAll(sink.requests());

        if (status != InstanceStatus.OK) {
            InstanceSnapshot snapshot = new InstanceSnapshot(
                    key,
                    status,
                    sink.diagnostics(),
                    Optional.empty(),
                    Optional.empty(),
                    interest
            );
            return new InstanceState(snapshot, Optional.of(bindings), Optional.empty(), Optional.empty(), interest);
        }

        // 3) Compile：产出 compiledContext。
        CompileReport compiled = schema.compiler().compile(new CompileContextImpl(key, world, directory, tick.budget(), sink, bindings));
        sink.merge(compiled);
        status = worse(status, compiled.status());
        interest = applyInterest(interest, sink.interestUpdate());
        frame.requests.addAll(sink.requests());

        if (status != InstanceStatus.OK || compiled.compiledContext().isEmpty()) {
            InstanceSnapshot snapshot = new InstanceSnapshot(
                    key,
                    status,
                    sink.diagnostics(),
                    Optional.empty(),
                    Optional.empty(),
                    interest
            );
            return new InstanceState(snapshot, Optional.of(bindings), compiled.compiledContext(), Optional.empty(), interest);
        }

        // 4) Declare deps：声明跨实例依赖边（仅声明，不读取）。
        DependencyReport deps = schema.dependencyDeclarer().declare(new DepContextImpl(key, world, directory, tick.budget(), sink, bindings, compiled.compiledContext().get()));
        sink.merge(deps);
        status = worse(status, deps.status());
        interest = applyInterest(interest, sink.interestUpdate());
        frame.requests.addAll(sink.requests());
        depEdgesByInstance.put(key, deps.edges());

        if (status != InstanceStatus.OK) {
            InstanceSnapshot snapshot = new InstanceSnapshot(
                    key,
                    status,
                    sink.diagnostics(),
                    Optional.empty(),
                    Optional.empty(),
                    interest
            );
            return new InstanceState(snapshot, Optional.of(bindings), compiled.compiledContext(), Optional.empty(), interest);
        }

        // 5) Link：读取依赖上下文并链接为最终上下文。
        LinkReport linked = schema.linker().link(new LinkContextImpl(key, world, directory, tick.budget(), sink, compiled.compiledContext().get()));
        sink.merge(linked);
        status = worse(status, linked.status());
        interest = applyInterest(interest, sink.interestUpdate());
        frame.requests.addAll(sink.requests());

        Optional<ExportView> export = Optional.empty();
        if (status == InstanceStatus.OK) {
            // 6) Export：导出数据访问视图（可选）。
            ExportReport exportReport = schema.exporter().export(new ExportContextImpl(key, world, directory, tick.budget(), sink, bindings, linked.linkedContext().orElse(compiled.compiledContext().get())));
            sink.merge(exportReport);
            status = worse(status, exportReport.status());
            interest = applyInterest(interest, sink.interestUpdate());
            export = exportReport.export();
        }

        InstanceSnapshot snapshot = new InstanceSnapshot(
                key,
                status,
                sink.diagnostics(),
                linked.linkedContext().or(() -> compiled.compiledContext()),
                export,
                interest
        );
        removeInterestIndex(key, instances.getOrDefault(key, InstanceState.empty(key)).interest());
        addInterestIndex(key, interest);
        return new InstanceState(snapshot, Optional.of(bindings), compiled.compiledContext(), linked.linkedContext(), interest);
    }

    private void applyCycleDiagnostics(FrameBuilder frame) {
        Set<InstanceKey> cycleNodes = detectAnyCycle();
        if (cycleNodes.isEmpty()) {
            return;
        }
        for (InstanceKey node : cycleNodes) {
            InstanceState state = instances.get(node);
            if (state == null) {
                continue;
            }
            List<Diagnostic> diags = new ArrayList<>(state.snapshot().diagnostics());
            diags.add(Diagnostic.of(DIAG_DEP_CYCLE, dev.vfyjxf.cumulus.api.diagnostic.Severity.ERROR, "Dependency cycle detected", DiagnosticEvidence.empty()));
            InstanceSnapshot snapshot = new InstanceSnapshot(
                    node,
                    InstanceStatus.ERROR,
                    diags,
                    state.snapshot().runtimeContext(),
                    state.snapshot().exportView(),
                    state.snapshot().interest()
            );
            instances.put(node, state.withSnapshot(snapshot));
            frame.deltas.add(InstanceDelta.updated(snapshot));
        }
    }

    private Set<InstanceKey> detectAnyCycle() {
        Map<InstanceKey, List<InstanceKey>> graph = new HashMap<>();
        for (Map.Entry<InstanceKey, List<DependencyEdge>> entry : depEdgesByInstance.entrySet()) {
            InstanceKey from = entry.getKey();
            List<InstanceKey> tos = entry.getValue().stream().map(DependencyEdge::to).toList();
            graph.put(from, tos);
        }

        Set<InstanceKey> visiting = new HashSet<>();
        Set<InstanceKey> visited = new HashSet<>();
        Set<InstanceKey> cycle = new HashSet<>();
        for (InstanceKey node : graph.keySet()) {
            if (dfsCycle(node, graph, visiting, visited, cycle)) {
                break;
            }
        }
        return cycle;
    }

    private boolean dfsCycle(InstanceKey node, Map<InstanceKey, List<InstanceKey>> graph, Set<InstanceKey> visiting, Set<InstanceKey> visited, Set<InstanceKey> cycle) {
        if (visited.contains(node)) {
            return false;
        }
        if (!visiting.add(node)) {
            cycle.add(node);
            return true;
        }
        for (InstanceKey next : graph.getOrDefault(node, List.of())) {
            if (dfsCycle(next, graph, visiting, visited, cycle)) {
                cycle.add(node);
                return true;
            }
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }

    private static InterestView applyInterest(InterestView current, InterestUpdate update) {
        if (update == null) {
            return current;
        }
        Set<SectionKey> sections = new HashSet<>(current.sections());
        sections.removeAll(update.sectionsRemove());
        sections.addAll(update.sectionsAdd());

        Set<BlockPos> watchPos = new HashSet<>(current.watchPositions());
        watchPos.removeAll(update.watchPositionsRemove());
        watchPos.addAll(update.watchPositionsAdd());

        Set<java.util.UUID> entities = new HashSet<>(current.watchEntities());
        entities.removeAll(update.watchEntitiesRemove());
        entities.addAll(update.watchEntitiesAdd());

        return new InterestView(sections, watchPos, entities, update.ttl().or(() -> current.ttl()));
    }

    private void addInterestIndex(InstanceKey key, InterestView interest) {
        for (SectionKey section : interest.sections()) {
            interestIndex.computeIfAbsent(section, __ -> new HashSet<>()).add(key);
        }
    }

    private void removeInterestIndex(InstanceKey key, InterestView interest) {
        for (SectionKey section : interest.sections()) {
            Set<InstanceKey> set = interestIndex.get(section);
            if (set != null) {
                set.remove(key);
                if (set.isEmpty()) {
                    interestIndex.remove(section);
                }
            }
        }
    }

    private static InstanceStatus worse(InstanceStatus a, InstanceStatus b) {
        if (a == InstanceStatus.ERROR || b == InstanceStatus.ERROR) {
            return InstanceStatus.ERROR;
        }
        if (a == InstanceStatus.INCOMPLETE || b == InstanceStatus.INCOMPLETE) {
            return InstanceStatus.INCOMPLETE;
        }
        return InstanceStatus.OK;
    }

    private static Collection<DataRequest> changeSetToRequests(ChangeSet changeSet) {
        // ChangeSet is an input hint; dummy engine does not convert it to requests.
        return List.of();
    }

    private static final class FrameBuilder {
        private final List<InstanceDelta> deltas = new ArrayList<>();
        private final Set<DataRequest> requests = new LinkedHashSet<>();

        private EngineFrame build() {
            return new EngineFrame(deltas, List.copyOf(requests));
        }
    }

    private record InstanceState(
            InstanceSnapshot snapshot,
            Optional<BindingsView> bindings,
            Optional<dev.vfyjxf.cumulus.api.runtime.RuntimeContextView> compiledContext,
            Optional<dev.vfyjxf.cumulus.api.runtime.RuntimeContextView> linkedContext,
            InterestView interest
    ) {
        public static InstanceState empty(InstanceKey key) {
            return new InstanceState(
                    new InstanceSnapshot(key, InstanceStatus.INCOMPLETE, List.of(), Optional.empty(), Optional.empty(), InterestView.empty()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    InterestView.empty()
            );
        }

        public InstanceState withSnapshot(InstanceSnapshot snapshot) {
            return new InstanceState(snapshot, bindings, compiledContext, linkedContext, interest);
        }
    }

    private static final class CollectingPassSink implements PassSink {
        private final List<DataRequest> requests = new ArrayList<>();
        private final List<Diagnostic> diagnostics = new ArrayList<>();
        private InterestUpdate interest = InterestUpdate.empty();

        @Override
        public void request(DataRequest request) {
            requests.add(Objects.requireNonNull(request, "request"));
        }

        @Override
        public void diag(Diagnostic diagnostic) {
            diagnostics.add(Objects.requireNonNull(diagnostic, "diagnostic"));
        }

        @Override
        public void interest(InterestUpdate update) {
            interest = mergeInterest(interest, Objects.requireNonNull(update, "update"));
        }

        public List<DataRequest> requests() {
            return List.copyOf(requests);
        }

        public List<Diagnostic> diagnostics() {
            return List.copyOf(diagnostics);
        }

        public InterestUpdate interestUpdate() {
            return interest;
        }

        public void merge(dev.vfyjxf.cumulus.api.pass.PassReport report) {
            diagnostics.addAll(report.diagnostics());
            requests.addAll(report.requests());
            interest = mergeInterest(interest, report.interestUpdate());
        }

        private static InterestUpdate mergeInterest(InterestUpdate a, InterestUpdate b) {
            Set<SectionKey> addSections = new HashSet<>(a.sectionsAdd());
            addSections.addAll(b.sectionsAdd());
            Set<SectionKey> removeSections = new HashSet<>(a.sectionsRemove());
            removeSections.addAll(b.sectionsRemove());

            Set<BlockPos> addPos = new HashSet<>(a.watchPositionsAdd());
            addPos.addAll(b.watchPositionsAdd());
            Set<BlockPos> removePos = new HashSet<>(a.watchPositionsRemove());
            removePos.addAll(b.watchPositionsRemove());

            Set<java.util.UUID> addEntities = new HashSet<>(a.watchEntitiesAdd());
            addEntities.addAll(b.watchEntitiesAdd());
            Set<java.util.UUID> removeEntities = new HashSet<>(a.watchEntitiesRemove());
            removeEntities.addAll(b.watchEntitiesRemove());

            return new InterestUpdate(addSections, removeSections, addPos, removePos, addEntities, removeEntities, b.ttl().isPresent() ? b.ttl() : a.ttl());
        }
    }

    private record DriverContextImpl(long tick, PartitionAccess world, EngineInstanceDirectory instances, Budget budget) implements DriverContext {
        private DriverContextImpl(TickContext tick, PartitionAccess world, InstanceDirectory instances) {
            this(tick.tick(), world, (EngineInstanceDirectory) instances, tick.budget());
        }
    }

    private record InstanceDirectoryView(Map<InstanceKey, InstanceState> instances) implements EngineInstanceDirectory {
        private InstanceDirectoryView {
            Objects.requireNonNull(instances, "instances");
        }

        @Override
        public Optional<dev.vfyjxf.cumulus.api.runtime.RuntimeContextView> context(InstanceKey key) {
            InstanceState state = instances.get(key);
            if (state == null) {
                return Optional.empty();
            }
            return state.snapshot.runtimeContext();
        }

        @Override
        public Optional<InstanceStatus> status(InstanceKey key) {
            InstanceState state = instances.get(key);
            if (state == null) {
                return Optional.empty();
            }
            return Optional.of(state.snapshot.status());
        }

        @Override
        public Iterable<InstanceView> views() {
            return instances.values().stream().map(state -> (InstanceView) state.snapshot()).toList();
        }
    }

    private record BindContextImpl(InstanceKey key, PartitionAccess world, InstanceDirectory instances, Budget budget, PassSink sink) implements BindContext {
        @Override
        public InstanceKey key() {
            return key;
        }

        @Override
        public PartitionAccess world() {
            return world;
        }

        @Override
        public InstanceDirectory instances() {
            return instances;
        }

        @Override
        public Budget budget() {
            return budget;
        }

        @Override
        public PassSink sink() {
            return sink;
        }
    }

    private record CheckContextImpl(InstanceKey key, PartitionAccess world, InstanceDirectory instances, Budget budget, PassSink sink, BindingsView bindings) implements CheckContext {
        @Override
        public InstanceKey key() {
            return key;
        }

        @Override
        public PartitionAccess world() {
            return world;
        }

        @Override
        public InstanceDirectory instances() {
            return instances;
        }

        @Override
        public Budget budget() {
            return budget;
        }

        @Override
        public PassSink sink() {
            return sink;
        }

        @Override
        public BindingsView bindings() {
            return bindings;
        }
    }

    private record CompileContextImpl(InstanceKey key, PartitionAccess world, InstanceDirectory instances, Budget budget, PassSink sink, BindingsView bindings) implements CompileContext {
        @Override
        public InstanceKey key() {
            return key;
        }

        @Override
        public PartitionAccess world() {
            return world;
        }

        @Override
        public InstanceDirectory instances() {
            return instances;
        }

        @Override
        public Budget budget() {
            return budget;
        }

        @Override
        public PassSink sink() {
            return sink;
        }

        @Override
        public BindingsView bindings() {
            return bindings;
        }
    }

    private record DepContextImpl(InstanceKey key, PartitionAccess world, InstanceDirectory instances, Budget budget, PassSink sink, BindingsView bindings, dev.vfyjxf.cumulus.api.runtime.RuntimeContextView compiledContext) implements DepContext {
        @Override
        public InstanceKey key() {
            return key;
        }

        @Override
        public PartitionAccess world() {
            return world;
        }

        @Override
        public InstanceDirectory instances() {
            return instances;
        }

        @Override
        public Budget budget() {
            return budget;
        }

        @Override
        public PassSink sink() {
            return sink;
        }

        @Override
        public BindingsView bindings() {
            return bindings;
        }

        @Override
        public dev.vfyjxf.cumulus.api.runtime.RuntimeContextView compiledContext() {
            return compiledContext;
        }
    }

    private record LinkContextImpl(InstanceKey key, PartitionAccess world, InstanceDirectory instances, Budget budget, PassSink sink, dev.vfyjxf.cumulus.api.runtime.RuntimeContextView compiledContext) implements LinkContext {
        @Override
        public InstanceKey key() {
            return key;
        }

        @Override
        public PartitionAccess world() {
            return world;
        }

        @Override
        public InstanceDirectory instances() {
            return instances;
        }

        @Override
        public Budget budget() {
            return budget;
        }

        @Override
        public PassSink sink() {
            return sink;
        }

        @Override
        public dev.vfyjxf.cumulus.api.runtime.RuntimeContextView compiledContext() {
            return compiledContext;
        }
    }

    private record ExportContextImpl(InstanceKey key, PartitionAccess world, InstanceDirectory instances, Budget budget, PassSink sink, BindingsView bindings, dev.vfyjxf.cumulus.api.runtime.RuntimeContextView runtimeContext) implements ExportContext {
        @Override
        public InstanceKey key() {
            return key;
        }

        @Override
        public PartitionAccess world() {
            return world;
        }

        @Override
        public InstanceDirectory instances() {
            return instances;
        }

        @Override
        public Budget budget() {
            return budget;
        }

        @Override
        public PassSink sink() {
            return sink;
        }

        @Override
        public BindingsView bindings() {
            return bindings;
        }

        @Override
        public dev.vfyjxf.cumulus.api.runtime.RuntimeContextView runtimeContext() {
            return runtimeContext;
        }
    }
}
