package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.predicate.PredicateRegistry;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import dev.vfyjxf.cumulus.api.request.RequestReason;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.visualization.VisualizationElement;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class StructureCheckExecutor {
    private static final ResourceLocation REGION_TARGET = ResourceLocation.parse("cumulus:region");
    private static final ResourceLocation CONTEXT_TARGET = ResourceLocation.parse("cumulus:context");
    private static final List<ResourceLocation> SUPPORTED_TARGETS = List.of(
            ResourceLocation.parse("cumulus:block"),
            ResourceLocation.parse("cumulus:block_entity"),
            ResourceLocation.parse("cumulus:entity"),
            ResourceLocation.parse("cumulus:fluid"),
            ResourceLocation.parse("cumulus:capability"),
            REGION_TARGET,
            CONTEXT_TARGET);

    private StructureCheckExecutor() {
    }

    public static StructureCheckResult check(StructureDefinition definition, BlockPos anchor, InMemoryTargetSnapshot snapshot) {
        return check(definition, anchor, snapshot, PredicateRegistry.defaults());
    }

    public static StructureCheckResult check(
            StructureDefinition definition,
            BlockPos anchor,
            InMemoryTargetSnapshot snapshot,
            PredicateRegistry predicateRegistry) {
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

    public static StructureCheckResult check(
            StructureCheckPlan plan,
            BlockPos anchor,
            InMemoryTargetSnapshot snapshot) {
        return check(plan, anchor, snapshot, PredicateRegistry.defaults());
    }

    public static StructureCheckResult check(
            StructureCheckPlan plan,
            BlockPos anchor,
            InMemoryTargetSnapshot snapshot,
            PredicateRegistry predicateRegistry) {
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
            if (result.status() == InstanceStatus.VALID) {
                return result;
            }
            if (result.status() == InstanceStatus.INCOMPLETE && firstIncomplete == null) {
                firstIncomplete = result;
            }
            if (result.status() == InstanceStatus.INVALID && firstInvalid == null) {
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

    public static FastCheckResult fastCheck(
            StructureCheckPlan plan,
            InMemoryTargetSnapshot snapshot) {
        return fastCheck(plan, snapshot, PredicateRegistry.defaults());
    }

    public static FastCheckResult fastCheck(
            StructureCheckPlan plan,
            TargetSnapshotSource source) {
        return fastCheck(plan, source, PredicateRegistry.defaults());
    }

    public static FastCheckResult fastCheck(
            StructureCheckPlan plan,
            InMemoryTargetSnapshot snapshot,
            PredicateRegistry predicateRegistry) {
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
            var result = fastCheckCandidate(
                    candidate,
                    (point, bounds) -> resolveTarget(point, bounds, snapshot),
                    predicateRegistry);
            candidatesEvaluated++;
            entriesEvaluated += result.entriesEvaluated();
            if (result.status() == InstanceStatus.VALID) {
                return new FastCheckResult(InstanceStatus.VALID, candidatesEvaluated, entriesEvaluated, candidateIndex);
            }
            if (result.status() == InstanceStatus.INCOMPLETE) {
                sawIncomplete = true;
            }
        }
        return new FastCheckResult(
                sawIncomplete ? InstanceStatus.INCOMPLETE : InstanceStatus.INVALID,
                candidatesEvaluated,
                entriesEvaluated,
                -1);
    }

    public static FastCheckResult fastCheck(
            StructureCheckPlan plan,
            TargetSnapshotSource source,
            PredicateRegistry predicateRegistry) {
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
            var result = fastCheckCandidate(
                    candidate,
                    (point, bounds) -> resolveTarget(point, bounds, source),
                    predicateRegistry);
            candidatesEvaluated++;
            entriesEvaluated += result.entriesEvaluated();
            if (result.status() == InstanceStatus.VALID) {
                return new FastCheckResult(InstanceStatus.VALID, candidatesEvaluated, entriesEvaluated, candidateIndex);
            }
            if (result.status() == InstanceStatus.INCOMPLETE) {
                sawIncomplete = true;
            }
        }
        return new FastCheckResult(
                sawIncomplete ? InstanceStatus.INCOMPLETE : InstanceStatus.INVALID,
                candidatesEvaluated,
                entriesEvaluated,
                -1);
    }

    private static FastCandidateResult fastCheckCandidate(
            CheckPlanCandidate candidate,
            FastTargetResolver targetResolver,
            PredicateRegistry predicateRegistry) {
        var entriesEvaluated = 0;
        var sawIncomplete = false;
        for (var entry : candidate.entries()) {
            entriesEvaluated++;
            var point = entry.point();
            var bounds = new Bounds(entry.min(), entry.max());
            if (!SUPPORTED_TARGETS.contains(point.target().typeId())) {
                if (point.requirement() == PointRequirement.OPTIONAL) {
                    continue;
                }
                return new FastCandidateResult(InstanceStatus.INVALID, entriesEvaluated);
            }
            var target = targetResolver.resolve(point, bounds);
            if (target == null) {
                if (point.requirement() == PointRequirement.OPTIONAL) {
                    continue;
                }
                sawIncomplete = true;
                continue;
            }
            var failed = evaluatePredicates(point, target, predicateRegistry);
            if (failed != null && point.requirement() != PointRequirement.OPTIONAL) {
                return new FastCandidateResult(InstanceStatus.INVALID, entriesEvaluated);
            }
        }
        return new FastCandidateResult(
                sawIncomplete ? InstanceStatus.INCOMPLETE : InstanceStatus.VALID,
                entriesEvaluated);
    }

    private static StructureCheckResult checkCandidate(
            StructureDefinition definition,
            BlockPos anchor,
            InMemoryTargetSnapshot snapshot,
            CheckPlanCandidate candidate,
            PredicateRegistry predicateRegistry) {
        var key = new StructureInstanceKey(definition.id(), anchor);
        var contextBuilder = RuntimeStructureContext.builder(key).revision(1);
        var nodes = new ArrayList<DiagnosticNode>();
        var edges = new ArrayList<DiagnosticEdge>();
        var requests = new ArrayList<DataRequest>();
        var visualization = new ArrayList<VisualizationElement>();

        var entries = candidate == null ? fallbackEntries(definition) : candidate.entries();
        for (var entry : entries) {
            var point = entry.point();
            var bounds = new Bounds(entry.min(), entry.max());
            if (!SUPPORTED_TARGETS.contains(point.target().typeId())) {
                if (point.requirement() == PointRequirement.OPTIONAL) {
                    continue;
                }
                addProblem(
                        point,
                        bounds,
                        "UNSUPPORTED_TARGET",
                        "Unsupported target",
                        "diagnostic.cumulus.unsupported_target",
                        null,
                        nodes,
                        edges,
                        visualization);
                continue;
            }
            var target = resolveTarget(point, bounds, snapshot);
            if (target == null) {
                if (point.requirement() == PointRequirement.OPTIONAL) {
                    continue;
                }
                var code = CONTEXT_TARGET.equals(point.target().typeId()) ? "MISSING_CONTEXT" : "MISSING_TARGET";
                var localizationKey = CONTEXT_TARGET.equals(point.target().typeId())
                        ? "diagnostic.cumulus.missing_context"
                        : "diagnostic.cumulus.missing_target";
                addProblem(point, bounds, code, "Missing target", localizationKey, null, nodes, edges, visualization);
                requests.add(new DataRequest(
                        SectionKey.fromBlock(anchor.offset(bounds.min().x(), bounds.min().y(), bounds.min().z())),
                        RequestReason.CHECK_NEEDS_CONTEXT,
                        RequestPriority.HIGH,
                        20,
                        definition.id() + "/" + point.id().value()));
                continue;
            }

            var failed = evaluatePredicates(point, target, predicateRegistry);
            if (failed != null) {
                if (point.requirement() == PointRequirement.OPTIONAL) {
                    continue;
                }
                addProblem(
                        point,
                        bounds,
                        failed.code(),
                        failed.message(),
                        failed.localizationKey(),
                        failed.predicate(),
                        nodes,
                        edges,
                        visualization);
                continue;
            }

            for (var capture : point.captures()) {
                var absolute = anchor.offset(bounds.min().x(), bounds.min().y(), bounds.min().z());
                contextBuilder.put(capture, absolute.getX() + "," + absolute.getY() + "," + absolute.getZ());
            }
        }

        var diagnostics = graph(nodes, edges);
        var status = status(nodes, requests);
        return new StructureCheckResult(
                status,
                contextBuilder.build(),
                diagnostics,
                new VisualizationModel(definition.id() + "/check", visualization),
                requests);
    }

    private static List<CheckPlanEntry> fallbackEntries(StructureDefinition definition) {
        return StructureRuntimePlanCompiler.compileCheckPlan(definition).candidates().stream()
                .findFirst()
                .map(CheckPlanCandidate::entries)
                .orElse(List.of());
    }

    private static PredicateProblem evaluatePredicates(
            StructurePointDefinition point,
            SnapshotTarget target,
            PredicateRegistry predicateRegistry) {
        for (var predicate : point.predicates()) {
            var evaluation = PredicateEvaluator.evaluate(predicate, target, predicateRegistry);
            if (evaluation == PredicateEvaluator.Evaluation.UNSUPPORTED) {
                return new PredicateProblem(
                        "UNSUPPORTED_PREDICATE",
                        "Unsupported predicate",
                        "diagnostic.cumulus.unsupported_predicate",
                        predicate);
            }
            if (evaluation == PredicateEvaluator.Evaluation.FAILED) {
                return new PredicateProblem(
                        "PREDICATE_FAILED",
                        "Predicate failed",
                        "diagnostic.cumulus.predicate_failed",
                        predicate);
            }
        }
        return null;
    }

    private static SnapshotTarget resolveTarget(
            StructurePointDefinition point,
            Bounds bounds,
            InMemoryTargetSnapshot snapshot) {
        if (CONTEXT_TARGET.equals(point.target().typeId())) {
            return snapshot.context(point.target().properties().get("key")).orElse(null);
        }
        return snapshot.target(point.target().typeId(), bounds.min(), bounds.max()).orElse(null);
    }

    private static SnapshotTarget resolveTarget(
            StructurePointDefinition point,
            Bounds bounds,
            TargetSnapshotSource source) {
        if (CONTEXT_TARGET.equals(point.target().typeId())) {
            var key = point.target().properties().get("key");
            if (key == null || key.isBlank()) {
                return null;
            }
            return source.context(key)
                    .map(value -> new SnapshotTarget(value, Map.of("key", key)))
                    .orElse(null);
        }
        return source.target(point.target().typeId(), bounds.min(), bounds.max(), point.target().properties()).orElse(null);
    }

    private static void addProblem(
            StructurePointDefinition point,
            Bounds bounds,
            String code,
            String message,
            String localizationKey,
            PredicateDescriptor predicate,
            List<DiagnosticNode> nodes,
            List<DiagnosticEdge> edges,
            List<VisualizationElement> visualization) {
        if (nodes.isEmpty()) {
            nodes.add(new DiagnosticNode(
                    "root",
                    Severity.ERROR,
                    "Structure check failed",
                    "diagnostic.cumulus.structure_failed",
                    List.of(),
                    "runtime/check",
                    List.of()));
        }
        var pointId = point.id().value();
        nodes.add(new DiagnosticNode(
                pointId,
                Severity.ERROR,
                message,
                localizationKey,
                List.of(DiagnosticEvidence.relative(
                        bounds.min(),
                        point.target().typeId().toString(),
                        predicate == null ? code : predicate.id().toString(),
                        pointId,
                        code,
                        descriptors(bounds, predicate))),
                "runtime/check/" + pointId,
                List.of()));
        edges.add(new DiagnosticEdge("root", pointId, "caused_by"));
        visualization.add(VisualizationElement.region(
                pointId + "-error",
                point.target().typeId().getPath(),
                bounds.min(),
                bounds.max(),
                Severity.ERROR,
                point.groups().isEmpty() ? "structure" : point.groups().iterator().next(),
                localizationKey,
                100));
    }

    private static Map<String, String> descriptors(Bounds bounds, PredicateDescriptor predicate) {
        var descriptors = new java.util.LinkedHashMap<String, String>();
        descriptors.put("to", bounds.max().asString());
        if (predicate != null) {
            for (var entry : predicate.properties().entrySet()) {
                descriptors.put("predicate." + entry.getKey(), entry.getValue());
            }
        }
        return descriptors;
    }

    private static DiagnosticGraph graph(List<DiagnosticNode> nodes, List<DiagnosticEdge> edges) {
        if (nodes.isEmpty()) {
            return new DiagnosticGraph("root", List.of(), List.of());
        }
        return new DiagnosticGraph("root", nodes, edges);
    }

    private static InstanceStatus status(List<DiagnosticNode> nodes, List<DataRequest> requests) {
        if (!requests.isEmpty()) {
            return InstanceStatus.INCOMPLETE;
        }
        if (!nodes.isEmpty()) {
            return InstanceStatus.INVALID;
        }
        return InstanceStatus.VALID;
    }

    private static RelativeBlockPos parsePos(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Relative position string must not be blank");
        }
        var parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Relative position must have x,y,z parts");
        }
        return RelativeBlockPos.of(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
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
