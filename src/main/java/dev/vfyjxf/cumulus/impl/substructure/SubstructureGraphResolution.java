package dev.vfyjxf.cumulus.impl.substructure;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import java.util.Optional;

public record SubstructureGraphResolution(
        Optional<SubstructureGraph> graph,
        DiagnosticGraph diagnostics) {
    public SubstructureGraphResolution {
        if (graph == null) {
            throw new IllegalArgumentException("Substructure graph optional must not be null");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Substructure diagnostics must not be null");
        }
    }

    public static SubstructureGraphResolution resolved(SubstructureGraph graph, DiagnosticGraph diagnostics) {
        return new SubstructureGraphResolution(Optional.of(graph), diagnostics);
    }

    public static SubstructureGraphResolution failed(DiagnosticGraph diagnostics) {
        return new SubstructureGraphResolution(Optional.empty(), diagnostics);
    }

    public boolean hasErrors() {
        return diagnostics.nodes().stream().anyMatch(node -> node.severity().ordinal()
                >= dev.vfyjxf.cumulus.api.diagnostic.Severity.error.ordinal());
    }
}
