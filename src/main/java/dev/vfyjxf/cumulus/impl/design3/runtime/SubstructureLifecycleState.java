package dev.vfyjxf.cumulus.impl.design3.runtime;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.impl.design3.check.StructureCheckResult;
import dev.vfyjxf.cumulus.impl.design3.substructure.RuntimeSubstructureOccurrence;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;

final class SubstructureLifecycleState {
    private static final Set<String> CHILD_PARENT_METADATA = Set.of(
            "parent_occurrence",
            "parent_structure",
            "parent_anchor",
            "parent_status");

    private SubstructureLifecycleState() {
    }

    static List<InstanceDelta> deltas(
            StructureInstanceKey rootInstance,
            StructureCheckResult rootResult,
            List<RuntimeSubstructureOccurrence> occurrences) {
        if (rootInstance == null) {
            throw new IllegalArgumentException("Root instance key must not be null");
        }
        if (rootResult == null) {
            throw new IllegalArgumentException("Root check result must not be null");
        }
        if (occurrences == null) {
            throw new IllegalArgumentException("Substructure occurrences must not be null");
        }

        var childDeltas = new ArrayList<InstanceDelta>();
        for (var occurrence : occurrences) {
            childDeltas.add(delta(rootInstance, rootResult, occurrence));
        }
        var deltas = new ArrayList<InstanceDelta>();
        deltas.add(rootDelta(rootInstance, rootResult, occurrences, childDeltas));
        deltas.addAll(childDeltas);
        return deltas;
    }

    static List<InstanceDelta> synchronizeRoot(
            StructureInstanceKey rootInstance,
            List<InstanceDelta> deltas,
            List<RuntimeSubstructureOccurrence> occurrences,
            SubstructureSyncPolicy syncPolicy) {
        if (rootInstance == null) {
            throw new IllegalArgumentException("Root instance key must not be null");
        }
        if (deltas == null) {
            throw new IllegalArgumentException("Instance deltas must not be null");
        }
        if (occurrences == null) {
            throw new IllegalArgumentException("Substructure occurrences must not be null");
        }
        if (syncPolicy == null) {
            throw new IllegalArgumentException("Substructure sync policy must not be null");
        }
        if (occurrences.isEmpty()) {
            return List.copyOf(deltas);
        }

        var childrenByOccurrence = new LinkedHashMap<String, InstanceDelta>();
        for (var delta : deltas) {
            var occurrenceId = delta.context().values().get("parent_occurrence");
            if (occurrenceId != null) {
                childrenByOccurrence.put(occurrenceId, delta);
            }
        }
        var synchronizedDeltas = new ArrayList<InstanceDelta>();
        for (var delta : deltas) {
            if (!delta.key().equals(rootInstance)) {
                synchronizedDeltas.add(delta);
                continue;
            }
            var context = RuntimeStructureContext.builder(rootInstance)
                    .revision(delta.context().revision());
            for (var entry : delta.context().values().entrySet()) {
                if (!isSubstructureSyncKey(entry.getKey(), occurrences)) {
                    context.put(entry.getKey(), entry.getValue());
                }
            }
            for (var occurrence : occurrences) {
                var child = childrenByOccurrence.get(occurrence.occurrenceId());
                if (child != null) {
                    putSynchronizedChildContext(context, occurrence, child, syncPolicy);
                }
            }
            synchronizedDeltas.add(new InstanceDelta(delta.key(), delta.status(), context.build()));
        }
        return List.copyOf(synchronizedDeltas);
    }

    private static InstanceDelta rootDelta(
            StructureInstanceKey rootInstance,
            StructureCheckResult rootResult,
            List<RuntimeSubstructureOccurrence> occurrences,
            List<InstanceDelta> childDeltas) {
        var context = RuntimeStructureContext.builder(rootInstance)
                .revision(rootResult.context().revision());
        for (var entry : rootResult.context().values().entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }
        for (var index = 0; index < occurrences.size(); index++) {
            putSynchronizedChildContext(
                    context,
                    occurrences.get(index),
                    childDeltas.get(index),
                    SubstructureSyncPolicy.STATUS_ONLY);
        }
        return new InstanceDelta(rootInstance, rootResult.status(), context.build());
    }

    static Map<StructureInstanceKey, DiagnosticGraph> diagnostics(
            BlockPos rootAnchor,
            StructureCheckResult rootResult,
            List<RuntimeSubstructureOccurrence> occurrences) {
        if (rootAnchor == null) {
            throw new IllegalArgumentException("Root anchor must not be null");
        }
        if (rootResult == null) {
            throw new IllegalArgumentException("Root check result must not be null");
        }
        if (occurrences == null) {
            throw new IllegalArgumentException("Substructure occurrences must not be null");
        }

        var diagnostics = new LinkedHashMap<StructureInstanceKey, DiagnosticGraph>();
        for (var occurrence : occurrences) {
            diagnostics.put(key(rootAnchor, occurrence), diagnosticFor(rootResult.diagnostics(), occurrence));
        }
        return diagnostics;
    }

    private static InstanceDelta delta(
            StructureInstanceKey rootInstance,
            StructureCheckResult rootResult,
            RuntimeSubstructureOccurrence occurrence) {
        var key = key(rootInstance.anchor(), occurrence);
        var context = RuntimeStructureContext.builder(key)
                .revision(rootResult.context().revision())
                .put("parent_occurrence", occurrence.occurrenceId())
                .put("parent_structure", rootInstance.structureId().toString())
                .put("parent_anchor", format(rootInstance.anchor()))
                .put("parent_status", rootResult.status().name());
        var prefix = occurrence.occurrenceId() + "/";
        for (var entry : rootResult.context().values().entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                context.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        return new InstanceDelta(key, statusFor(rootResult, occurrence), context.build());
    }

    private static void putSynchronizedChildContext(
            RuntimeStructureContext.Builder context,
            RuntimeSubstructureOccurrence occurrence,
            InstanceDelta childDelta,
            SubstructureSyncPolicy syncPolicy) {
        var prefix = "substructure." + occurrence.occurrenceId() + ".";
        context.put(prefix + "status", childDelta.status().name());
        if (syncPolicy != SubstructureSyncPolicy.STATUS_AND_CONTEXT) {
            return;
        }
        context.put(prefix + "anchor_offset", occurrence.anchorOffsetFromRoot().asString());
        var contextPrefix = prefix + "context.";
        for (var entry : childDelta.context().values().entrySet()) {
            if (!CHILD_PARENT_METADATA.contains(entry.getKey())) {
                context.put(contextPrefix + entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean isSubstructureSyncKey(
            String key,
            List<RuntimeSubstructureOccurrence> occurrences) {
        for (var occurrence : occurrences) {
            if (key.startsWith("substructure." + occurrence.occurrenceId() + ".")) {
                return true;
            }
        }
        return false;
    }

    private static StructureInstanceKey key(BlockPos rootAnchor, RuntimeSubstructureOccurrence occurrence) {
        var offset = occurrence.anchorOffsetFromRoot();
        return new StructureInstanceKey(
                occurrence.definitionId(),
                rootAnchor.offset(offset.x(), offset.y(), offset.z()));
    }

    private static InstanceStatus statusFor(
            StructureCheckResult rootResult,
            RuntimeSubstructureOccurrence occurrence) {
        if (rootResult.status() == InstanceStatus.VALID) {
            return InstanceStatus.VALID;
        }
        var occurrencePrefix = occurrence.occurrenceId() + "/";
        var hasOccurrenceDiagnostic = rootResult.diagnostics().nodes().stream()
                .filter(node -> !node.id().equals(rootResult.diagnostics().rootNodeId()))
                .anyMatch(node -> node.id().equals(occurrence.occurrenceId()) || node.id().startsWith(occurrencePrefix));
        var hasOccurrenceRequest = rootResult.requests().stream()
                .anyMatch(request -> request.consumerId().contains(occurrencePrefix));
        return hasOccurrenceDiagnostic || hasOccurrenceRequest ? rootResult.status() : InstanceStatus.VALID;
    }

    private static DiagnosticGraph diagnosticFor(
            DiagnosticGraph rootDiagnostics,
            RuntimeSubstructureOccurrence occurrence) {
        var occurrencePrefix = occurrence.occurrenceId() + "/";
        var nodes = rootDiagnostics.nodes().stream()
                .filter(node -> !node.id().equals(rootDiagnostics.rootNodeId()))
                .filter(node -> node.id().equals(occurrence.occurrenceId()) || node.id().startsWith(occurrencePrefix))
                .map(node -> scopedNode(node, occurrence))
                .toList();
        if (nodes.isEmpty()) {
            return new DiagnosticGraph("root", List.of(), List.of());
        }
        var edges = new ArrayList<DiagnosticEdge>();
        for (var node : nodes) {
            edges.add(new DiagnosticEdge("root", node.id(), "caused_by"));
        }
        return new DiagnosticGraph("root", nodes, edges);
    }

    private static DiagnosticNode scopedNode(DiagnosticNode node, RuntimeSubstructureOccurrence occurrence) {
        var occurrencePrefix = occurrence.occurrenceId() + "/";
        var scopedId = stripPrefix(node.id(), occurrencePrefix);
        return new DiagnosticNode(
                scopedId,
                node.severity(),
                node.message(),
                node.localizationKey(),
                node.evidence().stream()
                        .map(evidence -> scopedEvidence(evidence, occurrence))
                        .toList(),
                "runtime/substructures/" + scopedId,
                node.fixHints());
    }

    private static DiagnosticEvidence scopedEvidence(
            DiagnosticEvidence evidence,
            RuntimeSubstructureOccurrence occurrence) {
        var occurrencePrefix = occurrence.occurrenceId() + "/";
        var offset = occurrence.anchorOffsetFromRoot();
        return DiagnosticEvidence.relative(
                RelativeBlockPos.of(
                        evidence.relativePosition().x() - offset.x(),
                        evidence.relativePosition().y() - offset.y(),
                        evidence.relativePosition().z() - offset.z()),
                evidence.targetId(),
                evidence.predicateId(),
                stripPrefix(evidence.componentPath(), occurrencePrefix),
                evidence.detail(),
                evidence.descriptors());
    }

    private static String stripPrefix(String value, String prefix) {
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }

    private static String format(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
