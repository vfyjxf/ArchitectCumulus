package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.context.WellKnownContextKeys;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;
import dev.vfyjxf.cumulus.impl.check.StructureCheckExecutor;
import dev.vfyjxf.cumulus.impl.check.StructureCheckPlan;
import dev.vfyjxf.cumulus.impl.check.TargetSnapshotReader;
import dev.vfyjxf.cumulus.impl.check.TargetSnapshotSource;
import dev.vfyjxf.cumulus.impl.substructure.RuntimeSubstructureOccurrence;
import dev.vfyjxf.cumulus.impl.substructure.SubstructureGraphResolver;
import dev.vfyjxf.cumulus.impl.substructure.SubstructureRuntimeComposer;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class RuntimeCheckEngine {
    private final RuntimeScheduler scheduler = new RuntimeScheduler();
    private final Map<ResourceLocation, StructureDefinition> definitionsById = new LinkedHashMap<>();
    private final Map<StructureInstanceKey, TargetSnapshotSource> sourcesByInstance = new LinkedHashMap<>();
    private final Map<StructureInstanceKey, StructureDefinition> runtimeDefinitionsByInstance = new LinkedHashMap<>();
    private final Map<StructureInstanceKey, StructureCheckPlan> checkPlansByInstance = new LinkedHashMap<>();
    private final Map<StructureInstanceKey, DiagnosticGraph> diagnosticsByInstance = new LinkedHashMap<>();
    private final Map<StructureInstanceKey, ScheduledCheckResult> preflightResultsByInstance = new LinkedHashMap<>();
    private final Map<SubstructureLifecycleKey, InstanceStatus> substructureStatusesByInstance = new LinkedHashMap<>();
    private final Map<StructureInstanceKey, List<RuntimeSubstructureOccurrence>> substructureOccurrencesByInstance = new LinkedHashMap<>();
    private final SubstructureClaimState substructureClaimState = new SubstructureClaimState();
    private final List<Consumer<SubstructureLifecycleEvent>> substructureLifecycleListeners = new ArrayList<>();
    private SubstructureSyncPolicy substructureSyncPolicy = SubstructureSyncPolicy.statusOnly;

    public RuntimeCheckEngine substructureSyncPolicy(SubstructureSyncPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Substructure sync policy must not be null");
        }
        this.substructureSyncPolicy = policy;
        return this;
    }

    public RuntimeCheckEngine addSubstructureLifecycleListener(Consumer<SubstructureLifecycleEvent> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Substructure lifecycle listener must not be null");
        }
        substructureLifecycleListeners.add(listener);
        return this;
    }

    public RuntimeCheckEngine register(StructureDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        definitionsById.put(definition.id(), definition);
        return this;
    }

    public RuntimeCheckEngine track(ResourceLocation definitionId, BlockPos anchor, TargetSnapshotSource source) {
        if (definitionId == null) {
            throw new IllegalArgumentException("Definition id must not be null");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("Anchor must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Target snapshot source must not be null");
        }
        var definition = requireDefinition(definitionId);
        var key = new StructureInstanceKey(definitionId, anchor);
        clearSubstructureRuntimeState(key);
        var runtimeDefinition = resolveRuntimeDefinition(definition);
        if (runtimeDefinition.hasErrors()) {
            var context = RuntimeStructureContext.builder(key).revision(1).build();
            preflightResultsByInstance.put(key, new ScheduledCheckResult(
                            List.of(new InstanceDelta(key, InstanceStatus.invalid, context)),
                            List.of()));
            diagnosticsByInstance.put(key, runtimeDefinition.diagnostics());
            sourcesByInstance.remove(key);
            runtimeDefinitionsByInstance.remove(key);
            checkPlansByInstance.remove(key);
            scheduler.register(key, StructureRuntimePlanCompiler.compile(definition));
            scheduler.enqueue(key);
            return this;
        }
        var composedDefinition = runtimeDefinition.definition();
        var checkPlan = StructureRuntimePlanCompiler.compileCheckPlan(composedDefinition);
        scheduler.register(key, StructureRuntimePlanCompiler.compile(composedDefinition));
        sourcesByInstance.put(key, source);
        runtimeDefinitionsByInstance.put(key, composedDefinition);
        checkPlansByInstance.put(key, checkPlan);
        substructureOccurrencesByInstance.put(key, runtimeDefinition.substructureOccurrences());
        diagnosticsByInstance.remove(key);
        preflightResultsByInstance.remove(key);
        return this;
    }

    public void untrack(ResourceLocation definitionId, BlockPos anchor) {
        var key = new StructureInstanceKey(definitionId, anchor);
        scheduler.unregister(key);
        sourcesByInstance.remove(key);
        runtimeDefinitionsByInstance.remove(key);
        checkPlansByInstance.remove(key);
        clearSubstructureRuntimeState(key);
        diagnosticsByInstance.remove(key);
        preflightResultsByInstance.remove(key);
    }

    public void markDirty(SectionKey section) {
        scheduler.markDirty(section);
    }

    public RuntimeFrameResult tick(int tick, int maxChecks) {
        return scheduler.drainDue(tick, maxChecks, this::check);
    }

    public RuntimeFrameResult fastTick(int tick, int maxChecks) {
        return scheduler.drainDue(tick, maxChecks, this::fastCheck);
    }

    public int pendingChecks() {
        return scheduler.pendingChecks();
    }

    public int compiledCheckPlanCount() {
        return checkPlansByInstance.size();
    }

    public DiagnosticGraph diagnostics(StructureInstanceKey instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Structure instance key must not be null");
        }
        return diagnosticsByInstance.getOrDefault(instance, new DiagnosticGraph("root", List.of(), List.of()));
    }

    private ScheduledCheckResult check(StructureInstanceKey instance) {
        var preflightResult = preflightResultsByInstance.get(instance);
        if (preflightResult != null) {
            return preflightResult;
        }
        var definition = runtimeDefinitionsByInstance.getOrDefault(instance, requireDefinition(instance.structureId()));
        var checkPlan = checkPlansByInstance.get(instance);
        if (checkPlan == null) {
            checkPlan = StructureRuntimePlanCompiler.compileCheckPlan(definition);
            checkPlansByInstance.put(instance, checkPlan);
        }
        var source = sourcesByInstance.get(instance);
        if (source == null) {
            throw new IllegalStateException("No target snapshot source registered for " + instance);
        }
        var snapshot = TargetSnapshotReader.fromPlan(checkPlan, source);
        var result = StructureCheckExecutor.check(checkPlan, instance.anchor(), snapshot);
        diagnosticsByInstance.put(instance, result.diagnostics());
        diagnosticsByInstance.putAll(SubstructureLifecycleState.diagnostics(instance.anchor(), result, substructureOccurrencesByInstance.getOrDefault(instance, List.of())));
        var claimResult = substructureClaimState.resolve(SubstructureLifecycleState.deltas(instance, result, substructureOccurrencesByInstance.getOrDefault(instance, List.of())));
        var deltas = SubstructureLifecycleState.synchronizeRoot(instance, claimResult.deltas(), substructureOccurrencesByInstance.getOrDefault(instance, List.of()), substructureSyncPolicy);
        publishSubstructureLifecycleEvents(instance, deltas);
        diagnosticsByInstance.putAll(claimResult.diagnostics());
        return new ScheduledCheckResult(
                        deltas,
                        result.requests());
    }

    private ScheduledCheckResult fastCheck(StructureInstanceKey instance) {
        var preflightResult = preflightResultsByInstance.get(instance);
        if (preflightResult != null) {
            return new ScheduledCheckResult(
                            preflightResult.deltas(),
                            preflightResult.requests(),
                            List.of());
        }
        var definition = runtimeDefinitionsByInstance.getOrDefault(instance, requireDefinition(instance.structureId()));
        var checkPlan = checkPlansByInstance.get(instance);
        if (checkPlan == null) {
            checkPlan = StructureRuntimePlanCompiler.compileCheckPlan(definition);
            checkPlansByInstance.put(instance, checkPlan);
        }
        var source = sourcesByInstance.get(instance);
        if (source == null) {
            throw new IllegalStateException("No target snapshot source registered for " + instance);
        }
        return ScheduledCheckResult.fast(new RuntimeFastCheck(
                        instance,
                        StructureCheckExecutor.fastCheck(checkPlan, source)));
    }

    private RuntimeDefinitionResolution resolveRuntimeDefinition(StructureDefinition definition) {
        var resolution = SubstructureGraphResolver.resolve(definition.id(), definitionsById);
        if (resolution.hasErrors()) {
            return RuntimeDefinitionResolution.failed(resolution.diagnostics());
        }
        var graph = resolution.graph().orElseThrow();
        return RuntimeDefinitionResolution.resolved(SubstructureRuntimeComposer.compose(definition.id(), definitionsById, graph), SubstructureRuntimeComposer.occurrences(graph));
    }

    private StructureDefinition requireDefinition(ResourceLocation definitionId) {
        var definition = definitionsById.get(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("No structure definition registered for " + definitionId);
        }
        return definition;
    }

    private void publishSubstructureLifecycleEvents(StructureInstanceKey parent, List<InstanceDelta> deltas) {
        if (substructureLifecycleListeners.isEmpty()) {
            rememberSubstructureStatuses(parent, deltas);
            return;
        }
        for (var delta : deltas) {
            if (!isSubstructureDelta(delta, parent)) {
                continue;
            }
            var lifecycleKey = new SubstructureLifecycleKey(parent, delta.key());
            var previous = Optional.ofNullable(substructureStatusesByInstance.put(lifecycleKey, delta.status()));
            if (previous.isEmpty()) {
                publish(new SubstructureLifecycleEvent(
                                parent,
                                delta.key(),
                                delta.context().get(WellKnownContextKeys.parentOccurrence).orElse(null),
                                previous,
                                delta.status(),
                                SubstructureLifecycleTransition.added));
            } else if (previous.orElseThrow() != delta.status()) {
                publish(new SubstructureLifecycleEvent(
                                parent,
                                delta.key(),
                                delta.context().get(WellKnownContextKeys.parentOccurrence).orElse(null),
                                previous,
                                delta.status(),
                                SubstructureLifecycleTransition.statusChanged));
            }
        }
    }

    private void rememberSubstructureStatuses(StructureInstanceKey parent, List<InstanceDelta> deltas) {
        for (var delta : deltas) {
            if (isSubstructureDelta(delta, parent)) {
                substructureStatusesByInstance.put(new SubstructureLifecycleKey(parent, delta.key()), delta.status());
            }
        }
    }

    private void publish(SubstructureLifecycleEvent event) {
        for (var listener : substructureLifecycleListeners) {
            listener.accept(event);
        }
    }

    private void clearSubstructureRuntimeState(StructureInstanceKey parent) {
        var oldOccurrences = substructureOccurrencesByInstance.remove(parent);
        if (oldOccurrences != null) {
            for (var occurrence : oldOccurrences) {
                diagnosticsByInstance.remove(substructureKey(parent.anchor(), occurrence));
            }
        }
        substructureStatusesByInstance.keySet().removeIf(lifecycleKey -> lifecycleKey.parent().equals(parent));
    }

    private static boolean isSubstructureDelta(InstanceDelta delta, StructureInstanceKey parent) {
        var values = delta.context().values();
        return values.containsKey(WellKnownContextKeys.parentOccurrence) && values.containsKey(WellKnownContextKeys.parentStructure) && values.containsKey(WellKnownContextKeys.parentAnchor) && parentKey(values).equals(parent);
    }

    private static StructureInstanceKey parentKey(Map<ContextKey<?>, Object> values) {
        var structure = (String) values.get(WellKnownContextKeys.parentStructure);
        var anchor = (String) values.get(WellKnownContextKeys.parentAnchor);
        return new StructureInstanceKey(
                        ResourceLocation.parse(structure),
                        parseBlockPos(anchor));
    }

    private static BlockPos parseBlockPos(String value) {
        var parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Parent anchor must have x,y,z parts");
        }
        return new BlockPos(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
    }

    private static StructureInstanceKey substructureKey(BlockPos rootAnchor, RuntimeSubstructureOccurrence occurrence) {
        var offset = occurrence.anchorOffsetFromRoot();
        return new StructureInstanceKey(
                        occurrence.definitionId(),
                        rootAnchor.offset(offset.x(), offset.y(), offset.z()));
    }

    private record SubstructureLifecycleKey(
                    StructureInstanceKey parent,
                    StructureInstanceKey child) {
        private SubstructureLifecycleKey {
            if (parent == null) {
                throw new IllegalArgumentException("Parent instance key must not be null");
            }
            if (child == null) {
                throw new IllegalArgumentException("Child instance key must not be null");
            }
        }
    }

    private record RuntimeDefinitionResolution(
                    StructureDefinition definition,
                    DiagnosticGraph diagnostics,
                    List<RuntimeSubstructureOccurrence> substructureOccurrences) {
        private RuntimeDefinitionResolution {
            substructureOccurrences = List.copyOf(substructureOccurrences);
        }

        private static RuntimeDefinitionResolution resolved(StructureDefinition definition, List<RuntimeSubstructureOccurrence> substructureOccurrences) {
            return new RuntimeDefinitionResolution(
                            definition,
                            new DiagnosticGraph("root", List.of(), List.of()),
                            substructureOccurrences);
        }

        private static RuntimeDefinitionResolution failed(DiagnosticGraph diagnostics) {
            return new RuntimeDefinitionResolution(null, diagnostics, List.of());
        }

        private boolean hasErrors() {
            return diagnostics.nodes().stream().anyMatch(node -> node.severity().ordinal() >= dev.vfyjxf.cumulus.api.diagnostic.Severity.error.ordinal());
        }
    }
}
