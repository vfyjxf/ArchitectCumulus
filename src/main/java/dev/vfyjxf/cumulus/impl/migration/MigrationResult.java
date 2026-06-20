package dev.vfyjxf.cumulus.impl.migration;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.migration.MigrationDescriptor;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;

import java.util.List;

public record MigrationResult(
                int version,
                RuntimeStructureContext context,
                List<MigrationDescriptor> appliedMigrations,
                DiagnosticGraph diagnostics) {
    public MigrationResult {
        if (version < 1) {
            throw new IllegalArgumentException("Migration result version must be positive");
        }
        if (context == null) {
            throw new IllegalArgumentException("Migrated context must not be null");
        }
        appliedMigrations = List.copyOf(appliedMigrations);
        if (diagnostics == null) {
            throw new IllegalArgumentException("Migration diagnostics must not be null");
        }
    }
}
