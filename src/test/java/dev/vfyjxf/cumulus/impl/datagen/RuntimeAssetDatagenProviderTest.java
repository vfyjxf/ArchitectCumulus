package dev.vfyjxf.cumulus.impl.datagen;

import dev.vfyjxf.cumulus.api.datagen.DataDrivenStructureAsset;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeAssetDatagenProviderTest {
    @Test
    void exportsMultipleRuntimeAssetsWithDeterministicPathsAndContent() {
        var controller = assetPlan("cumulus:machines/controller", "cumulus:controller");
        var casing = assetPlan("cumulus:machines/casing", "cumulus:casing");

        var plan = RuntimeAssetExportSetPlan.of(List.of(casing, controller));

        var exported = new RuntimeAssetDatagenProvider().export(plan);

        assertEquals(List.of("data/cumulus/cumulus/structures/machines/casing.json", "data/cumulus/cumulus/structures/machines/controller.json"), exported.stream().map(asset -> asset.resourcePath()).toList());
        assertEquals("{\"id\":\"cumulus:machines/casing\",\"schema_version\":1,\"definition\":\"cumulus:casing\",\"debug_asset\":false,\"runtime_plan\":{\"definition\":\"cumulus:casing\",\"entry_count\":1,\"entries\":[{\"id\":\"origin\",\"target\":\"cumulus:block\",\"min\":\"0,0,0\",\"max\":\"0,0,0\",\"properties\":{}}],\"regions\":[]}}", exported.get(0).serialized());
        assertEquals("{\"id\":\"cumulus:machines/controller\",\"schema_version\":1,\"definition\":\"cumulus:controller\",\"debug_asset\":false,\"runtime_plan\":{\"definition\":\"cumulus:controller\",\"entry_count\":1,\"entries\":[{\"id\":\"origin\",\"target\":\"cumulus:block\",\"min\":\"0,0,0\",\"max\":\"0,0,0\",\"properties\":{}}],\"regions\":[]}}", exported.get(1).serialized());
    }

    @Test
    void rejectsDuplicateResourcePathsBeforeExporting() {
        var first = assetPlan("cumulus:machines/duplicate", "cumulus:first");
        var second = assetPlan("cumulus:machines/duplicate", "cumulus:second");

        var error = assertThrows(IllegalArgumentException.class, () -> RuntimeAssetExportSetPlan.of(List.of(first, second)));

        assertEquals("Duplicate runtime asset resource path: data/cumulus/cumulus/structures/machines/duplicate.json", error.getMessage());
    }

    @Test
    void preservesStableOutputOrderFromThePlan() {
        var beta = assetPlan("cumulus:ordered/beta", "cumulus:beta");
        var alpha = assetPlan("cumulus:ordered/alpha", "cumulus:alpha");
        var gamma = assetPlan("other:ordered/gamma", "other:gamma");

        var plan = RuntimeAssetExportSetPlan.of(List.of(beta, alpha, gamma));

        assertEquals(List.of("data/cumulus/cumulus/structures/ordered/beta.json", "data/cumulus/cumulus/structures/ordered/alpha.json", "data/other/cumulus/structures/ordered/gamma.json"), new RuntimeAssetDatagenProvider().export(plan).stream().map(asset -> asset.resourcePath()).toList());
    }

    private static RuntimeAssetExportTask assetPlan(String runtimeAssetId, String definitionId) {
        var definition = Structures.define(ResourceLocation.parse(definitionId)).point("origin", RelativeBlockPos.of(0, 0, 0), Targets.block()).build();
        return new RuntimeAssetExportTask(
                        new DataDrivenStructureAsset(
                                        ResourceLocation.parse(runtimeAssetId),
                                        1,
                                        ResourceLocation.parse(definitionId),
                                        false),
                        StructureRuntimePlanCompiler.compile(definition));
    }
}
