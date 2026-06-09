package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.ownership.ClaimConflict;
import dev.vfyjxf.cumulus.api.ownership.ClaimMode;
import dev.vfyjxf.cumulus.api.ownership.PartClaim;
import dev.vfyjxf.cumulus.api.ownership.StructureClaimIndex;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

final class SubstructureClaimState {
    private static final ResourceLocation SUBSTRUCTURE_PART =
            ResourceLocation.parse("cumulus:substructure_instance");

    private final StructureClaimIndex claims = new StructureClaimIndex();

    ClaimResult resolve(List<InstanceDelta> deltas) {
        var resolved = new java.util.ArrayList<InstanceDelta>();
        var diagnostics = new java.util.LinkedHashMap<StructureInstanceKey, DiagnosticGraph>();
        for (var delta : deltas) {
            if (isChildOccurrence(delta) && delta.status() == InstanceStatus.VALID) {
                var claim = new PartClaim(partId(delta.key()), parentOwner(delta), ClaimMode.EXCLUSIVE);
                var conflict = claims.conflict(claim);
                if (conflict.isPresent()) {
                    resolved.add(new InstanceDelta(delta.key(), InstanceStatus.INVALID, delta.context()));
                    diagnostics.put(delta.key(), conflictDiagnostics(conflict.orElseThrow()));
                    continue;
                }
                claims.claim(claim);
            }
            resolved.add(delta);
        }
        return new ClaimResult(resolved, diagnostics);
    }

    private static boolean isChildOccurrence(InstanceDelta delta) {
        return delta.context().values().containsKey("parent_occurrence");
    }

    private static ResourceLocation partId(StructureInstanceKey key) {
        return ResourceLocation.fromNamespaceAndPath(
                SUBSTRUCTURE_PART.getNamespace(),
                key.structureId().getNamespace()
                        + "/" + key.structureId().getPath()
                        + "/" + key.anchor().getX()
                        + "_" + key.anchor().getY()
                        + "_" + key.anchor().getZ());
    }

    private static StructureInstanceKey parentOwner(InstanceDelta delta) {
        var values = delta.context().values();
        return new StructureInstanceKey(
                ResourceLocation.parse(values.get("parent_structure")),
                parseBlockPos(values.get("parent_anchor")));
    }

    private static net.minecraft.core.BlockPos parseBlockPos(String value) {
        var parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Parent anchor must have x,y,z parts");
        }
        return new net.minecraft.core.BlockPos(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
    }

    private static DiagnosticGraph conflictDiagnostics(ClaimConflict conflict) {
        var existing = conflict.existing();
        var attempted = conflict.attempted();
        var root = new DiagnosticNode(
                "root",
                Severity.ERROR,
                "Substructure ownership conflict",
                "diagnostic.cumulus.substructure_ownership_conflict",
                List.of(),
                "runtime/substructures/ownership",
                List.of());
        var conflictNode = new DiagnosticNode(
                "ownership-conflict",
                Severity.ERROR,
                "Substructure instance ownership conflict",
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
                "runtime/substructures/ownership/" + attempted.partId(),
                List.of());
        return new DiagnosticGraph(
                "root",
                List.of(root, conflictNode),
                List.of(new DiagnosticEdge("root", "ownership-conflict", "caused_by")));
    }

    record ClaimResult(
            List<InstanceDelta> deltas,
            Map<StructureInstanceKey, DiagnosticGraph> diagnostics) {
        ClaimResult {
            deltas = List.copyOf(deltas);
            diagnostics = Map.copyOf(diagnostics);
        }
    }
}
