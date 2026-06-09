package dev.vfyjxf.cumulus.impl.design3;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.ownership.ClaimConflict;
import dev.vfyjxf.cumulus.api.ownership.PartClaim;
import dev.vfyjxf.cumulus.api.ownership.StructureClaimIndex;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import dev.vfyjxf.cumulus.api.request.RequestReason;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.impl.design3.check.InMemoryTargetSnapshot;
import dev.vfyjxf.cumulus.impl.design3.check.StructureCheckExecutor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class StructureInstanceManager {
    private final Map<ResourceLocation, StructureRuntimePlan> plansById = new HashMap<>();
    private final Map<StructureInstanceKey, RuntimeStructureContext> contextsByKey = new HashMap<>();
    private final Map<StructureInstanceKey, DiagnosticGraph> diagnosticsByKey = new HashMap<>();
    private final StructureClaimIndex claimIndex = new StructureClaimIndex();

    public void register(StructureRuntimePlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Runtime plan must not be null");
        }
        plansById.put(plan.definitionId(), plan);
    }

    public EngineFrame track(ResourceLocation definitionId, BlockPos anchor) {
        var plan = requirePlan(definitionId);
        var key = new StructureInstanceKey(definitionId, anchor);
        var context = contextsByKey.computeIfAbsent(key, ignored -> RuntimeStructureContext.builder(key).build());
        var requests = new ArrayList<DataRequest>();
        for (var section : sortedSections(plan, anchor)) {
            requests.add(new DataRequest(
                    section,
                    RequestReason.PREFETCH_INCOMPLETE_INSTANCE,
                    RequestPriority.NORMAL,
                    20,
                    definitionId + "/footprint"));
        }
        return new EngineFrame(List.of(new InstanceDelta(key, InstanceStatus.INCOMPLETE, context)), requests);
    }

    private static List<SectionKey> sortedSections(StructureRuntimePlan plan, BlockPos anchor) {
        return plan.footprintSections(anchor).stream()
                .sorted(Comparator
                        .comparing(SectionKey::x)
                        .thenComparing(SectionKey::y)
                        .thenComparing(SectionKey::z))
                .toList();
    }

    public EngineFrame publishContext(ResourceLocation definitionId, BlockPos anchor, Map<String, String> values) {
        requirePlan(definitionId);
        var key = new StructureInstanceKey(definitionId, anchor);
        var previous = contextsByKey.get(key);
        var revision = previous == null ? 1 : previous.revision() + 1;
        var builder = RuntimeStructureContext.builder(key).revision(revision);
        for (var entry : values.entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }
        var context = builder.build();
        contextsByKey.put(key, context);
        return new EngineFrame(List.of(new InstanceDelta(key, InstanceStatus.VALID, context)), List.of());
    }

    public EngineFrame check(StructureDefinition definition, BlockPos anchor, InMemoryTargetSnapshot snapshot) {
        return check(definition, anchor, snapshot, List.of());
    }

    public EngineFrame check(
            StructureDefinition definition,
            BlockPos anchor,
            InMemoryTargetSnapshot snapshot,
            List<PartClaim> claims) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        if (claims == null) {
            throw new IllegalArgumentException("Part claims must not be null");
        }
        requirePlan(definition.id());
        var result = StructureCheckExecutor.check(definition, anchor, snapshot);
        var key = result.context().key();
        contextsByKey.put(key, result.context());
        if (result.status() != InstanceStatus.VALID) {
            diagnosticsByKey.put(key, result.diagnostics());
            return new EngineFrame(
                    List.of(new InstanceDelta(key, result.status(), result.context())),
                    result.requests());
        }

        var conflict = firstConflict(key, claims);
        var status = conflict == null ? result.status() : InstanceStatus.INVALID;
        var diagnostics = conflict == null ? result.diagnostics() : conflictDiagnostics(conflict);
        diagnosticsByKey.put(key, diagnostics);
        if (conflict == null) {
            for (var claim : claims) {
                claimIndex.claim(claim);
            }
        }

        return new EngineFrame(
                List.of(new InstanceDelta(key, status, result.context())),
                result.requests());
    }

    public DiagnosticGraph diagnostics(StructureInstanceKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Instance key must not be null");
        }
        return diagnosticsByKey.getOrDefault(key, emptyDiagnostics());
    }

    private ClaimConflict firstConflict(StructureInstanceKey key, List<PartClaim> claims) {
        for (var claim : claims) {
            if (claim == null) {
                throw new IllegalArgumentException("Part claim must not be null");
            }
            if (!claim.owner().equals(key)) {
                throw new IllegalArgumentException("Part claim owner must match checked instance");
            }
            var conflict = claimIndex.conflict(claim);
            if (conflict.isPresent()) {
                return conflict.orElseThrow();
            }
        }
        return null;
    }

    private static DiagnosticGraph conflictDiagnostics(ClaimConflict conflict) {
        var existing = conflict.existing();
        var attempted = conflict.attempted();
        var root = new DiagnosticNode(
                "root",
                Severity.ERROR,
                "Structure ownership conflict",
                "diagnostic.cumulus.structure_ownership_conflict",
                List.of(),
                "runtime/ownership",
                List.of());
        var conflictNode = new DiagnosticNode(
                "ownership-conflict",
                Severity.ERROR,
                "Part ownership conflict",
                "diagnostic.cumulus.ownership_conflict",
                List.of(DiagnosticEvidence.relative(
                        RelativeBlockPos.of(0, 0, 0),
                        attempted.partId().toString(),
                        "cumulus:ownership",
                        attempted.partId().toString(),
                        "OWNERSHIP_CONFLICT",
                        Map.of(
                                "part", attempted.partId().toString(),
                                "existing.owner", existing.owner().toString(),
                                "existing.mode", existing.mode().name(),
                                "attempted.owner", attempted.owner().toString(),
                                "attempted.mode", attempted.mode().name()))),
                "runtime/ownership/" + attempted.partId(),
                List.of());
        return new DiagnosticGraph(
                "root",
                List.of(root, conflictNode),
                List.of(new DiagnosticEdge("root", "ownership-conflict", "caused_by")));
    }

    private static DiagnosticGraph emptyDiagnostics() {
        return new DiagnosticGraph("root", List.of(), List.of());
    }

    private StructureRuntimePlan requirePlan(ResourceLocation definitionId) {
        if (definitionId == null) {
            throw new IllegalArgumentException("Definition id must not be null");
        }
        var plan = plansById.get(definitionId);
        if (plan == null) {
            throw new IllegalArgumentException("No runtime plan registered for " + definitionId);
        }
        return plan;
    }
}
