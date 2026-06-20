package dev.vfyjxf.cumulus.impl.substructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

public record SubstructureGraph(
                SubstructureGraphNode root,
                List<SubstructureGraphNode> nodes,
                List<SubstructureGraphEdge> edges) {
    public SubstructureGraph {
        if (root == null) {
            throw new IllegalArgumentException("Substructure graph root must not be null");
        }
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public Optional<SubstructureGraphNode> node(String occurrenceId) {
        if (occurrenceId == null || occurrenceId.isBlank()) {
            throw new IllegalArgumentException("Substructure occurrence id must not be blank");
        }
        return nodes.stream().filter(node -> node.occurrenceId().equals(occurrenceId)).findFirst();
    }

    public SequencedMap<String, SubstructureGraphNode> nodesByOccurrenceId() {
        var byId = new LinkedHashMap<String, SubstructureGraphNode>();
        for (var node : nodes) {
            byId.put(node.occurrenceId(), node);
        }
        return byId;
    }
}
