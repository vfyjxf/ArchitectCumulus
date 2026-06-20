package dev.vfyjxf.cumulus.impl.migration;

import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.api.migration.MigrationDescriptor;
import dev.vfyjxf.cumulus.api.migration.StructureVersionDescriptor;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructureContextMigrationExecutorTest {
    @Test
    void appliesDirectMigrationOverRuntimeContextValues() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .version(new StructureVersionDescriptor(1, ResourceLocation.parse("cumulus:machine_v1")))
                .version(new StructureVersionDescriptor(2, ResourceLocation.parse("cumulus:machine")))
                .migration(rename(1, 2, "cumulus:migrations/machine_1_to_2", "facing", "orientation"))
                .build();
        var context = context(java.util.Map.of(
                ContextKey.string("facing"), "north",
                ContextKey.string("controller"), "1,64,1"));

        var result = StructureContextMigrationExecutor.migrate(definition, context, 1, 2);

        assertEquals(2, result.version());
        assertEquals(1, result.appliedMigrations().size());
        assertEquals(ResourceLocation.parse("cumulus:migrations/machine_1_to_2"),
                result.appliedMigrations().getFirst().migrationId());
        assertEquals(Map.of(
                ContextKey.string("orientation"), "north",
                ContextKey.string("controller"), "1,64,1"),
                result.context().values());
        assertEquals(1, result.context().revision());
        assertEquals(0, result.diagnostics().nodes().size());
    }

    @Test
    void appliesChainedMigrationPathToTargetVersion() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .version(new StructureVersionDescriptor(1, ResourceLocation.parse("cumulus:machine_v1")))
                .version(new StructureVersionDescriptor(2, ResourceLocation.parse("cumulus:machine_v2")))
                .version(new StructureVersionDescriptor(3, ResourceLocation.parse("cumulus:machine")))
                .migration(rename(1, 2, "cumulus:migrations/machine_1_to_2", "facing", "orientation"))
                .migration(rename(2, 3, "cumulus:migrations/machine_2_to_3", "orientation", "direction"))
                .build();
        var context = context(java.util.Map.of(ContextKey.string("facing"), "east"));

        var result = StructureContextMigrationExecutor.migrate(definition, context, 1, 3);

        assertEquals(3, result.version());
        assertEquals(List.of(
                        ResourceLocation.parse("cumulus:migrations/machine_1_to_2"),
                        ResourceLocation.parse("cumulus:migrations/machine_2_to_3")),
                result.appliedMigrations().stream().map(MigrationDescriptor::migrationId).toList());
        assertEquals(java.util.Map.of(ContextKey.string("direction"), "east"), result.context().values());
    }

    @Test
    void missingMigrationPathFailsWithDiagnostic() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .version(new StructureVersionDescriptor(1, ResourceLocation.parse("cumulus:machine_v1")))
                .version(new StructureVersionDescriptor(3, ResourceLocation.parse("cumulus:machine")))
                .migration(rename(1, 2, "cumulus:migrations/machine_1_to_2", "facing", "orientation"))
                .build();
        var context = context(java.util.Map.of(ContextKey.string("facing"), "south"));

        var error = assertThrows(MigrationPathException.class,
                () -> StructureContextMigrationExecutor.migrate(definition, context, 1, 3));

        assertEquals("No migration path from version 1 to 3 for cumulus:machine", error.getMessage());
        assertEquals("root", error.diagnostics().rootNodeId());
        assertEquals("missing-migration-path", error.diagnostics().childrenOf("root").getFirst().id());
        var evidence = error.diagnostics().childrenOf("root").getFirst().evidence().getFirst();
        assertEquals("MISSING_MIGRATION_PATH", evidence.detail());
        assertEquals("1", evidence.descriptors().get("from"));
        assertEquals("3", evidence.descriptors().get("to"));
    }

    @Test
    void propertyDrivenRenameMovesContextKeyAndPreservesOtherValues() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .migration(new MigrationDescriptor(
                        2,
                        4,
                        ResourceLocation.parse("cumulus:migrations/rename_owner"),
                        Map.of(
                                "operation", "rename_context",
                                "from_context", "owner",
                                "to_context", "primary_owner")))
                .build();
        var context = context(java.util.Map.of(
                ContextKey.string("owner"), "alex",
                ContextKey.string("facing"), "west"));

        var result = StructureContextMigrationExecutor.migrate(definition, context, 2, 4);

        assertEquals(Map.of(
                ContextKey.string("primary_owner"), "alex",
                ContextKey.string("facing"), "west"),
                result.context().values());
        assertEquals(1, result.context().revision());
    }

    @Test
    void alreadyAtTargetVersionReturnsNoOpResult() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .migration(rename(1, 2, "cumulus:migrations/machine_1_to_2", "facing", "orientation"))
                .build();
        var context = context(java.util.Map.of(ContextKey.string("facing"), "north"));

        var result = StructureContextMigrationExecutor.migrate(definition, context, 2, 2);

        assertEquals(2, result.version());
        assertSame(context, result.context());
        assertEquals(List.of(), result.appliedMigrations());
        assertEquals(0, result.diagnostics().nodes().size());
    }

    private static MigrationDescriptor rename(int fromVersion, int toVersion, String id, String fromKey, String toKey) {
        return new MigrationDescriptor(
                fromVersion,
                toVersion,
                ResourceLocation.parse(id),
                Map.of(
                        "operation", "rename_context",
                        "from_context", fromKey,
                        "to_context", toKey));
    }

    private static RuntimeStructureContext context(java.util.Map<ContextKey<?>, Object> values) {
        var builder = RuntimeStructureContext.builder(new StructureInstanceKey(
                ResourceLocation.parse("cumulus:machine"),
                new BlockPos(1, 64, 1))).revision(1);
        builder.putAll(values);
        return builder.build();
    }
}
