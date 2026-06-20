package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.sources.CapturePositionSource;
import dev.vfyjxf.cumulus.api.context.sources.CapturePropertySource;
import dev.vfyjxf.cumulus.api.context.sources.CaptureValueSource;
import dev.vfyjxf.cumulus.api.context.sources.ContextRefSource;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * One-shot migrator from IR schema v1 to v2.
 */
public final class V1ToV2AssetMigrator {

    private V1ToV2AssetMigrator() {
    }

    public static boolean needsMigration(StructureDefinition definition) {
        return definition.schemaVersion() < StructureDefinition.IR_SCHEMA_VERSION;
    }

    /**
     * Pass-through: context rules are already typed at this point.
     * The v1→typed conversion happens in {@link LegacySourceParser} at load time.
     */
    public static List<ContextRuleDescriptor<?>> migrateContextRules(
            List<ContextRuleDescriptor<?>> rules) {
        return List.copyOf(rules);
    }
}
