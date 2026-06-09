package dev.vfyjxf.cumulus.impl.migration;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;

public final class MigrationPathException extends RuntimeException {
    private final DiagnosticGraph diagnostics;

    public MigrationPathException(String message, DiagnosticGraph diagnostics) {
        super(message);
        if (diagnostics == null) {
            throw new IllegalArgumentException("Migration diagnostics must not be null");
        }
        this.diagnostics = diagnostics;
    }

    public DiagnosticGraph diagnostics() {
        return diagnostics;
    }
}
