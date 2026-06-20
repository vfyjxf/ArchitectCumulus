package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DiagnosticGraph(
                String rootNodeId,
                List<DiagnosticNode> nodes,
                List<DiagnosticEdge> edges) {
    public DiagnosticGraph {
        if (rootNodeId == null || rootNodeId.isBlank()) {
            throw new IllegalArgumentException("Diagnostic graph root node id must not be blank");
        }
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public List<DiagnosticNode> childrenOf(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("Diagnostic parent node id must not be blank");
        }
        Map<String, DiagnosticNode> nodesById = new LinkedHashMap<>();
        for (var node : nodes) {
            nodesById.put(node.id(), node);
        }
        return edges.stream().filter(edge -> edge.fromNodeId().equals(nodeId)).map(DiagnosticEdge::toNodeId).map(nodesById::get).filter(node -> node != null).toList();
    }
}
