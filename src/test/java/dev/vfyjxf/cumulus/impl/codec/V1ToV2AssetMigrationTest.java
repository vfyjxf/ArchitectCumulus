package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.context.ContextSources;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.types.ContextKey;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class V1ToV2AssetMigrationTest {

    @Test
    void typedContextRulesPassThroughMigration() {
        var typedRule = ContextRuleDescriptor.derive(ContextKey.string("facing"), ContextSources.capture("controller").property("facing"));

        var migrated = V1ToV2AssetMigrator.migrateContextRules(List.of(typedRule));

        assertEquals(1, migrated.size());
        assertEquals(typedRule.source(), migrated.get(0).source());
    }

    @Test
    void needsMigrationDetectsOldSchemaVersion() {
        var currentDef = Structures.define(ResourceLocation.parse("cumulus:current")).point("p", RelativeBlockPos.of(0, 0, 0), Targets.block()).build();
        assertFalse(V1ToV2AssetMigrator.needsMigration(currentDef));
    }

    @Test
    void legacySourceParserConvertsDottedExpressionsToTypedSources() {
        var source = LegacySourceParser.parse("controller.facing");
        assertEquals(dev.vfyjxf.cumulus.api.context.sources.CapturePropertySource.class, source.getClass());

        var valueSource = LegacySourceParser.parse("controller.value");
        assertEquals(dev.vfyjxf.cumulus.api.context.sources.CaptureValueSource.class, valueSource.getClass());

        var posSource = LegacySourceParser.parse("controller.position");
        assertEquals(dev.vfyjxf.cumulus.api.context.sources.CapturePositionSource.class, posSource.getClass());

        var refSource = LegacySourceParser.parse("some_key");
        assertEquals(dev.vfyjxf.cumulus.api.context.sources.ContextRefSource.class, refSource.getClass());
    }
}
