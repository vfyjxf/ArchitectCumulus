package dev.vfyjxf.cumulus.impl.datagen;

import dev.vfyjxf.cumulus.impl.ExportedRuntimeAsset;
import dev.vfyjxf.cumulus.impl.RuntimeAssetExporter;

import java.util.List;

public final class RuntimeAssetDatagenProvider {
    public List<ExportedRuntimeAsset> export(RuntimeAssetExportSetPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Runtime asset export set plan must not be null");
        }

        return plan.tasks().stream().map(task -> RuntimeAssetExporter.export(task.asset(), task.plan())).toList();
    }
}
