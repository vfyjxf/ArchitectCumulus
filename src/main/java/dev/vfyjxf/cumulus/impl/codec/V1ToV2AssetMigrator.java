package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;

import java.util.List;

/**
 * One-shot migrator from IR schema v1 to v2.
 */
public final class V1ToV2AssetMigrator {

    private V1ToV2AssetMigrator() {}

    public static boolean needsMigration(StructureDefinition definition) {
        return definition.schemaVersion() < StructureDefinition.irSchemaVersion;
    }

    /**
     * Pass-through: context rules are already typed at this point.
     * The v1→typed conversion happens in {@link LegacySourceParser} at load time.
     */
    public static List<ContextRuleDescriptor<?>> migrateContextRules(List<ContextRuleDescriptor<?>> rules) {
        return List.copyOf(rules);
    }
}
