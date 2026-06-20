package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;

import java.util.List;

public record RuntimeAssetLoadResult(
                List<LoadedRuntimeAsset> assets,
                DiagnosticGraph diagnostics) {
    public RuntimeAssetLoadResult {
        assets = List.copyOf(assets);
        if (diagnostics == null) {
            throw new IllegalArgumentException("Runtime asset load diagnostics must not be null");
        }
    }

    public boolean valid() {
        return diagnostics.nodes().stream().noneMatch(node -> node.severity() == dev.vfyjxf.cumulus.api.diagnostic.Severity.error);
    }
}
