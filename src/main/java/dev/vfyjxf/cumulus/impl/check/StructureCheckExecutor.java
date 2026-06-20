package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.context.plan.MatchResults;
import dev.vfyjxf.cumulus.api.context.plan.MatchedPoint;
import dev.vfyjxf.cumulus.api.context.plan.PhaseContext;
import dev.vfyjxf.cumulus.api.context.plan.PhaseResult;
import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.predicate.PredicateRegistry;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import dev.vfyjxf.cumulus.api.request.RequestReason;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.target.TargetTypes;
import dev.vfyjxf.cumulus.api.target.types.ContextTargetType;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.api.visualization.VisualizationElement;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StructureCheckExecutor {

    // Typed dispatch: use TargetTypes registry instead of hardcoded ResourceLocation list
    private static boolean isSupportedTarget(ResourceLocation typeId) {
        return TargetTypes.isRegistered(typeId);
    }

    private StructureCheckExecutor() {}

    public static StructureCheckResult check(StructureDefinition definition, BlockPos anchor, InMemoryTargetSnapshot snapshot) {
        return check(definition, anchor, snapshot, PredicateRegistry.defaults());
    }

    public static StructureCheckResult check(StructureDefinition definition, BlockPos anchor, InMemoryTargetSnapshot snapshot, PredicateRegistry predicateRegistry) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("Anchor must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("Target snapshot must not be null");
        }
        if (predicateRegistry == null) {
            throw new IllegalArgumentException("Predicate registry must not be null");
        }
        return check(StructureRuntimePlanCompiler.compileCheckPlan(definition), anchor, snapshot, predicateRegistry);
    }

    public static StructureCheckResult check(StructureCheckPlan plan, BlockPos anchor, InMemoryTargetSnapshot snapshot) {
        return check(plan, anchor, snapshot, PredicateRegistry.defaults());
    }

    public static StructureCheckResult check(StructureCheckPlan plan, BlockPos anchor, InMemoryTargetSnapshot snapshot, PredicateRegistry predicateRegistry) {
        if (plan == null) {
            throw new IllegalArgumentException("Structure check plan must not be null");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("Anchor must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("Target snapshot must not be null");
        }
        if (predicateRegistry == null) {
            throw new IllegalArgumentException("Predicate registry must not be null");
        }

        StructureCheckResult firstInvalid = null;
        StructureCheckResult firstIncomplete = null;
        for (var candidate : plan.candidates()) {
            var result = checkCandidate(plan.definition(), anchor, snapshot, candidate, predicateRegistry);
            if (result.status() == InstanceStatus.valid) {
                return result;
            }
            if (result.status() == InstanceStatus.incomplete && firstIncomplete == null) {
                firstIncomplete = result;
            }
            if (result.status() == InstanceStatus.invalid && firstInvalid == null) {
                firstInvalid = result;
            }
        }
        if (firstIncomplete != null) {
            return firstIncomplete;
        }
        if (firstInvalid != null) {
            return firstInvalid;
        }
        return checkCandidate(plan.definition(), anchor, snapshot, null, predicateRegistry);
    }

    public static FastCheckResult fastCheck(StructureCheckPlan plan, InMemoryTargetSnapshot snapshot) {
        return fastCheck(plan, snapshot, PredicateRegistry.defaults());
    }

    public static FastCheckResult fastCheck(StructureCheckPlan plan, TargetSnapshotSource source) {
        return fastCheck(plan, source, PredicateRegistry.defaults());
    }

    public static FastCheckResult fastCheck(StructureCheckPlan plan, InMemoryTargetSnapshot snapshot, PredicateRegistry predicateRegistry) {
        if (plan == null) {
            throw new IllegalArgumentException("Structure check plan must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("Target snapshot must not be null");
        }
        if (predicateRegistry == null) {
            throw new IllegalArgumentException("Predicate registry must not be null");
        }

        var candidatesEvaluated = 0;
        var entriesEvaluated = 0;
        var sawIncomplete = false;
        for (var candidateIndex = 0; candidateIndex < plan.candidates().size(); candidateIndex++) {
            var candidate = plan.candidates().get(candidateIndex);
            var result = fastCheckCandidate(candidate, (point, bounds) -> resolveTarget(point, bounds, snapshot), predicateRegistry);
            candidatesEvaluated++;
            entriesEvaluated += result.entriesEvaluated();
            if (result.status() == InstanceStatus.valid) {
                return new FastCheckResult(InstanceStatus.valid, candidatesEvaluated, entriesEvaluated, candidateIndex);
            }
            if (result.status() == InstanceStatus.incomplete) {
                sawIncomplete = true;
            }
        }
        return new FastCheckResult(
                        sawIncomplete ? InstanceStatus.incomplete : InstanceStatus.invalid,
                        candidatesEvaluated,
                        entriesEvaluated,
                        -1);
    }

    public static FastCheckResult fastCheck(StructureCheckPlan plan, TargetSnapshotSource source, PredicateRegistry predicateRegistry) {
        if (plan == null) {
            throw new IllegalArgumentException("Structure check plan must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Target snapshot source must not be null");
        }
        if (predicateRegistry == null) {
            throw new IllegalArgumentException("Predicate registry must not be null");
        }

        var candidatesEvaluated = 0;
        var entriesEvaluated = 0;
        var sawIncomplete = false;
        for (var candidateIndex = 0; candidateIndex < plan.candidates().size(); candidateIndex++) {
            var candidate = plan.candidates().get(candidateIndex);
            var result = fastCheckCandidate(candidate, (point, bounds) -> resolveTarget(point, bounds, source), predicateRegistry);
            candidatesEvaluated++;
            entriesEvaluated += result.entriesEvaluated();
            if (result.status() == InstanceStatus.valid) {
                return new FastCheckResult(InstanceStatus.valid, candidatesEvaluated, entriesEvaluated, candidateIndex);
            }
            if (result.status() == InstanceStatus.incomplete) {
                sawIncomplete = true;
            }
        }
        return new FastCheckResult(
                        sawIncomplete ? InstanceStatus.incomplete : InstanceStatus.invalid,
                        candidatesEvaluated,
                        entriesEvaluated,
                        -1);
    }

    private static FastCandidateResult fastCheckCandidate(CheckPlanCandidate candidate, FastTargetResolver targetResolver, PredicateRegistry predicateRegistry) {
        var entriesEvaluated = 0;
        var sawIncomplete = false;
        for (var entry : candidate.entries()) {
            entriesEvaluated++;
            var point = entry.point();
            var bounds = new Bounds(entry.min(), entry.max());
            if (!isSupportedTarget(point.target().typeId())) {
                if (point.requirement() == PointRequirement.optional) {
                    continue;
                }
                return new FastCandidateResult(InstanceStatus.invalid, entriesEvaluated);
            }
            var target = targetResolver.resolve(point, bounds);
            if (target == null) {
                if (point.requirement() == PointRequirement.optional) {
                    continue;
                }
                sawIncomplete = true;
                continue;
            }
            var failed = evaluatePredicates(point, target, predicateRegistry);
            if (failed != null && point.requirement() != PointRequirement.optional) {
                return new FastCandidateResult(InstanceStatus.invalid, entriesEvaluated);
            }
        }
        return new FastCandidateResult(
                        sawIncomplete ? InstanceStatus.incomplete : InstanceStatus.valid,
                        entriesEvaluated);
    }

    private static StructureCheckResult checkCandidate(StructureDefinition definition, BlockPos anchor, InMemoryTargetSnapshot snapshot, CheckPlanCandidate candidate, PredicateRegistry predicateRegistry) {
        var key = new StructureInstanceKey(definition.id(), anchor);
        var checkCtx = new CheckContext();
        var nodes = new ArrayList<DiagnosticNode>();
        var edges = new ArrayList<DiagnosticEdge>();
        var requests = new ArrayList<DataRequest>();
        var visualization = new ArrayList<VisualizationElement>();
        var matchedPoints = new ArrayList<MatchedPoint>();

        var entries = candidate == null ? fallbackEntries(definition) : candidate.entries();
        var transform = candidate == null ? null : candidate.transform();
        for (var entry : entries) {
            matchPoint(entry.point(), new Bounds(entry.min(), entry.max()), definition, anchor, snapshot, predicateRegistry, checkCtx, nodes, edges, requests, visualization, matchedPoints);
        }
        applyContextRules(definition.contextRules(), checkCtx);

        // ── CheckPhase: dynamic point expansion + constraints ───────────
        if (definition.checkPhase() != null) {
            var matchResults = new MatchResults(matchedPoints);
            var phaseCtx = new PhaseContext(matchResults, checkCtx, anchor,
                            transform != null ? transform : Transforms.identity());
            PhaseResult phaseResult;
            try {
                phaseResult = definition.checkPhase().evaluate(phaseCtx);
            } catch (RuntimeException e) {
                phaseResult = PhaseResult.empty();
            }
            if (phaseResult == null) {
                phaseResult = PhaseResult.empty();
            }
            // Match extra points
            for (var extraPoint : phaseResult.extraPoints()) {
                var extraBounds = new Bounds(extraPoint.location(), extraPoint.location());
                matchPoint(extraPoint, extraBounds, definition, anchor, snapshot, predicateRegistry, checkCtx, nodes, edges, requests, visualization, matchedPoints);
            }
            // Verify count constraints
            var finalMatches = new MatchResults(matchedPoints);
            for (var constraint : phaseResult.constraints()) {
                int count = (int) finalMatches.count(constraint.filter());
                if (!constraint.isSatisfied(count)) {
                    nodes.add(new DiagnosticNode(
                                    "constraint_violation",
                                    Severity.error,
                                    "Count constraint violated: expected " + constraint.min() + "~" + constraint.max() + " matching points, got " + count,
                                    "diagnostic.cumulus.constraint_violation",
                                    List.of(),
                                    "runtime/check/constraint",
                                    List.of()));
                    if (nodes.size() > 1) {
                        edges.add(new DiagnosticEdge("root", "constraint_violation", "caused_by"));
                    }
                }
            }
        }

        var diagnostics = graph(nodes, edges);
        var status = status(nodes, requests);
        return new StructureCheckResult(
                        status,
                        checkCtx.toRuntime(key, 1),
                        diagnostics,
                        new VisualizationModel(definition.id() + "/check", visualization),
                        requests);
    }

    /**
     * Match a single point against the world snapshot: resolve target, evaluate
     * predicates, record captures, and build a {@link MatchedPoint} for
     * downstream phases/operations. Shared by static entries and dynamic
     * CheckPhase expansion.
     */
    private static void matchPoint(StructurePointDefinition point, Bounds bounds, StructureDefinition definition, BlockPos anchor, InMemoryTargetSnapshot snapshot, PredicateRegistry predicateRegistry, CheckContext checkCtx, List<DiagnosticNode> nodes, List<DiagnosticEdge> edges, List<DataRequest> requests, List<VisualizationElement> visualization, List<MatchedPoint> matchedPoints) {
        if (!isSupportedTarget(point.target().typeId())) {
            if (point.requirement() == PointRequirement.optional) {
                return;
            }
            addProblem(point, bounds, "WRONG_TARGET_TYPE", "Unsupported target", "diagnostic.cumulus.wrong_target_type", null, nodes, edges, visualization);
            return;
        }
        var target = resolveTarget(point, bounds, snapshot);
        if (target == null) {
            if (point.requirement() == PointRequirement.optional) {
                return;
            }
            var code = ContextTargetType.instance.id().equals(point.target().typeId()) ? "CONTEXT_CAPTURE_FAILED" : "MISSING_TARGET";
            var localizationKey = ContextTargetType.instance.id().equals(point.target().typeId()) ? "diagnostic.cumulus.context_capture_failed" : "diagnostic.cumulus.missing_target";
            addProblem(point, bounds, code, "Missing target", localizationKey, null, nodes, edges, visualization);
            requests.add(new DataRequest(
                            SectionKey.fromBlock(anchor.offset(bounds.min().x(), bounds.min().y(), bounds.min().z())),
                            RequestReason.checkNeedsContext,
                            RequestPriority.high,
                            20,
                            definition.id() + "/" + point.id().value()));
            return;
        }

        var failed = evaluatePredicates(point, target, predicateRegistry);
        if (failed != null) {
            if (point.requirement() == PointRequirement.optional) {
                return;
            }
            addProblem(point, bounds, failed.code(), failed.message(), failed.localizationKey(), failed.predicate(), nodes, edges, visualization);
            return;
        }

        var absolute = anchor.offset(bounds.min().x(), bounds.min().y(), bounds.min().z());
        for (var capture : point.captures()) {
            var formatted = format(absolute);
            checkCtx.putCapture(capture, formatted);
            checkCtx.putDerived(ContextKey.string(capture), formatted);
        }
        checkCtx.putResolvedTarget(point.id(), new CheckContext.ResolvedTarget(target.value(), target.properties(), format(absolute)));
        matchedPoints.add(new MatchedPoint(
                        point.id(),
                        point.groups(),
                        target.value(),
                        target.properties(),
                        bounds.min(),
                        absolute));
    }

    private static List<CheckPlanEntry> fallbackEntries(StructureDefinition definition) {
        return StructureRuntimePlanCompiler.compileCheckPlan(definition).candidates().stream().findFirst().map(CheckPlanCandidate::entries).orElse(List.of());
    }

    private static PredicateProblem evaluatePredicates(StructurePointDefinition point, SnapshotTarget target, PredicateRegistry predicateRegistry) {
        for (var predicate : point.predicates()) {
            var evaluation = PredicateEvaluator.evaluate(predicate, target, predicateRegistry);
            if (evaluation == PredicateEvaluator.Evaluation.unsupported) {
                return new PredicateProblem(
                                "UNSUPPORTED_PREDICATE",
                                "Unsupported predicate",
                                "diagnostic.cumulus.unsupported_predicate",
                                predicate);
            }
            if (evaluation == PredicateEvaluator.Evaluation.failed) {
                return new PredicateProblem(
                                "PREDICATE_FAILED",
                                "Predicate failed",
                                "diagnostic.cumulus.predicate_failed",
                                predicate);
            }
        }
        return null;
    }

    private static SnapshotTarget resolveTarget(StructurePointDefinition point, Bounds bounds, InMemoryTargetSnapshot snapshot) {
        if (ContextTargetType.instance.id().equals(point.target().typeId())) {
            return snapshot.context(point.target().properties().get("key")).orElse(null);
        }
        return snapshot.target(point.target().typeId(), bounds.min(), bounds.max()).orElse(null);
    }

    private static SnapshotTarget resolveTarget(StructurePointDefinition point, Bounds bounds, TargetSnapshotSource source) {
        if (ContextTargetType.instance.id().equals(point.target().typeId())) {
            var key = point.target().properties().get("key");
            if (key == null || key.isBlank()) {
                return null;
            }
            return source.context(key).map(value -> new SnapshotTarget(value, Map.of("key", key))).orElse(null);
        }
        return source.target(point.target().typeId(), bounds.min(), bounds.max(), point.target().properties()).orElse(null);
    }

    private static void addProblem(StructurePointDefinition point, Bounds bounds, String code, String message, String localizationKey, PredicateDescriptor predicate, List<DiagnosticNode> nodes, List<DiagnosticEdge> edges, List<VisualizationElement> visualization) {
        if (nodes.isEmpty()) {
            nodes.add(new DiagnosticNode(
                            "root",
                            Severity.error,
                            "Structure check failed",
                            "diagnostic.cumulus.structure_failed",
                            List.of(),
                            "runtime/check",
                            List.of()));
        }
        var pointId = point.id().value();
        nodes.add(new DiagnosticNode(
                        pointId,
                        Severity.error,
                        message,
                        localizationKey,
                        List.of(DiagnosticEvidence.relative(bounds.min(), point.target().typeId().toString(), predicate == null ? code : predicate.id().toString(), pointId, code, descriptors(bounds, predicate, point))),
                        "runtime/check/" + pointId,
                        List.of()));
        edges.add(new DiagnosticEdge("root", pointId, "caused_by"));
        visualization.add(VisualizationElement.region(pointId + "-error", point.target().typeId().getPath(), bounds.min(), bounds.max(), Severity.error, point.groups().isEmpty() ? "structure" : point.groups().iterator().next(), localizationKey, 100));
    }

    private static Map<String, String> descriptors(Bounds bounds, PredicateDescriptor predicate, StructurePointDefinition point) {
        var descriptors = new LinkedHashMap<String, String>();
        descriptors.put("to", bounds.max().asString());
        // Include point's DiagnosticMetadata for richer diagnostics
        if (!point.diagnostics().isEmpty()) {
            for (var entry : point.diagnostics().properties().entrySet()) {
                descriptors.put("meta." + entry.getKey(), entry.getValue());
            }
        }
        if (predicate != null) {
            for (var entry : predicate.properties().entrySet()) {
                descriptors.put("predicate." + entry.getKey(), entry.getValue());
            }
        }
        return descriptors;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyContextRules(List<ContextRuleDescriptor<?>> rules, CheckContext checkCtx) {
        for (var rule : rules) {
            var raw = (ContextRuleDescriptor) rule;
            raw.source().resolve(checkCtx).ifPresent(value -> checkCtx.putDerived(raw.targetKey(), value));
        }
    }

    private static DiagnosticGraph graph(List<DiagnosticNode> nodes, List<DiagnosticEdge> edges) {
        if (nodes.isEmpty()) {
            return new DiagnosticGraph("root", List.of(), List.of());
        }
        return new DiagnosticGraph("root", nodes, edges);
    }

    private static InstanceStatus status(List<DiagnosticNode> nodes, List<DataRequest> requests) {
        if (!requests.isEmpty()) {
            return InstanceStatus.incomplete;
        }
        if (!nodes.isEmpty()) {
            return InstanceStatus.invalid;
        }
        return InstanceStatus.valid;
    }

    private static String format(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private record Bounds(RelativeBlockPos min, RelativeBlockPos max) {
    }

    private record PredicateProblem(
                    String code,
                    String message,
                    String localizationKey,
                    PredicateDescriptor predicate) {
    }

    private record FastCandidateResult(
                    InstanceStatus status,
                    int entriesEvaluated) {
    }

    @FunctionalInterface
    private interface FastTargetResolver {
        SnapshotTarget resolve(StructurePointDefinition point, Bounds bounds);
    }
}
