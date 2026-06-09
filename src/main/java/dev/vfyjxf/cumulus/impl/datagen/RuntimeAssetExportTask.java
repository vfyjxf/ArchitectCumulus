package dev.vfyjxf.cumulus.impl.datagen;

import dev.vfyjxf.cumulus.api.datagen.DataDrivenStructureAsset;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;

public record RuntimeAssetExportTask(
        DataDrivenStructureAsset asset,
        StructureRuntimePlan plan) {
    public RuntimeAssetExportTask {
        if (asset == null) {
            throw new IllegalArgumentException("Runtime asset export task asset must not be null");
        }
        if (plan == null) {
            throw new IllegalArgumentException("Runtime asset export task plan must not be null");
        }
    }
}
