package dev.vfyjxf.cumulus.impl.migration;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.migration.MigrationDescriptor;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StructureContextMigrationExecutor {
    private static final String RENAME_CONTEXT = "rename_context";

    private StructureContextMigrationExecutor() {
    }

    public static MigrationResult migrate(
            StructureDefinition definition,
            RuntimeStructureContext context,
            int currentVersion,
            int targetVersion) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Runtime context must not be null");
        }
        if (currentVersion < 1) {
            throw new IllegalArgumentException("Current version must be positive");
        }
        if (targetVersion < 1) {
            throw new IllegalArgumentException("Target version must be positive");
        }
        if (currentVersion > targetVersion) {
            throw new IllegalArgumentException("Target version must not be older than current version");
        }
        if (currentVersion == targetVersion) {
            return new MigrationResult(targetVersion, context, List.of(), emptyDiagnostics());
        }

        var path = migrationPath(definition, currentVersion, targetVersion);
        var values = new LinkedHashMap<>(context.values());
        for (var migration : path) {
            apply(migration, values);
        }

        var migrated = rebuild(context, values);
        return new MigrationResult(targetVersion, migrated, path, emptyDiagnostics());
    }

    private static List<MigrationDescriptor> migrationPath(
            StructureDefinition definition,
            int currentVersion,
            int targetVersion) {
        var migrations = definition.migrations().stream()
                .sorted(Comparator
                        .comparingInt(MigrationDescriptor::fromVersion)
                        .thenComparingInt(MigrationDescriptor::toVersion)
                        .thenComparing(migration -> migration.migrationId().toString()))
                .toList();
        var path = new ArrayList<MigrationDescriptor>();
        var version = currentVersion;
        while (version < targetVersion) {
            var step = nextStep(migrations, version, targetVersion);
            if (step == null) {
                throw missingPath(definition, currentVersion, targetVersion, version);
            }
            path.add(step);
            version = step.toVersion();
        }
        return path;
    }

    private static MigrationDescriptor nextStep(
            List<MigrationDescriptor> migrations,
            int version,
            int targetVersion) {
        return migrations.stream()
                .filter(migration -> migration.fromVersion() == version)
                .filter(migration -> migration.toVersion() <= targetVersion)
                .min(Comparator
                        .comparingInt(MigrationDescriptor::toVersion)
                        .thenComparing(migration -> migration.migrationId().toString()))
                .orElse(null);
    }

    private static void apply(MigrationDescriptor migration, LinkedHashMap<String, String> values) {
        var operation = migration.properties().get("operation");
        if (operation == null || operation.isBlank()) {
            operation = RENAME_CONTEXT;
        }
        if (!RENAME_CONTEXT.equals(operation)) {
            throw unsupportedMigration(migration, "Unsupported migration operation " + operation);
        }

        var from = migration.properties().get("from_context");
        var to = migration.properties().get("to_context");
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw unsupportedMigration(migration, "Context rename migration requires from_context and to_context");
        }

        if (!values.containsKey(from)) {
            var defaultValue = migration.properties().get("default_value");
            if (defaultValue == null) {
                return;
            }
            values.put(to, defaultValue);
            return;
        }

        var value = values.remove(from);
        values.put(to, value);
    }

    private static RuntimeStructureContext rebuild(RuntimeStructureContext context, LinkedHashMap<String, String> values) {
        var builder = RuntimeStructureContext.builder(context.key()).revision(context.revision());
        for (var entry : values.entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private static MigrationPathException missingPath(
            StructureDefinition definition,
            int currentVersion,
            int targetVersion,
            int missingFromVersion) {
        var message = "No migration path from version " + currentVersion
                + " to " + targetVersion
                + " for " + definition.id();
        var descriptors = new LinkedHashMap<String, String>();
        descriptors.put("definition", definition.id().toString());
        descriptors.put("from", String.valueOf(currentVersion));
        descriptors.put("to", String.valueOf(targetVersion));
        descriptors.put("missing_from", String.valueOf(missingFromVersion));
        return new MigrationPathException(message, diagnostics(
                "missing-migration-path",
                "Structure migration path is incomplete",
                "diagnostic.cumulus.missing_migration_path",
                "MISSING_MIGRATION_PATH",
                descriptors));
    }

    private static MigrationPathException unsupportedMigration(MigrationDescriptor migration, String message) {
        var descriptors = new LinkedHashMap<String, String>();
        descriptors.put("migration", migration.migrationId().toString());
        descriptors.put("from", String.valueOf(migration.fromVersion()));
        descriptors.put("to", String.valueOf(migration.toVersion()));
        descriptors.putAll(migration.properties());
        return new MigrationPathException(message, diagnostics(
                "unsupported-migration",
                "Structure migration descriptor is unsupported",
                "diagnostic.cumulus.unsupported_migration",
                "UNSUPPORTED_MIGRATION",
                descriptors));
    }

    private static DiagnosticGraph diagnostics(
            String nodeId,
            String message,
            String localizationKey,
            String detail,
            Map<String, String> descriptors) {
        var root = new DiagnosticNode(
                "root",
                Severity.ERROR,
                "Structure migration failed",
                "diagnostic.cumulus.structure_migration_failed",
                List.of(),
                "runtime/migration",
                List.of());
        var node = new DiagnosticNode(
                nodeId,
                Severity.ERROR,
                message,
                localizationKey,
                List.of(DiagnosticEvidence.relative(
                        RelativeBlockPos.of(0, 0, 0),
                        "cumulus:migration",
                        "cumulus:migration",
                        "runtime/migration",
                        detail,
                        descriptors)),
                "runtime/migration/" + nodeId,
                List.of());
        return new DiagnosticGraph(
                "root",
                List.of(root, node),
                List.of(new DiagnosticEdge("root", nodeId, "caused_by")));
    }

    private static DiagnosticGraph emptyDiagnostics() {
        return new DiagnosticGraph("root", List.of(), List.of());
    }
}
