package dev.vfyjxf.cumulus.impl.datagen;

import java.util.LinkedHashSet;
import java.util.List;

public record RuntimeAssetExportSetPlan(List<RuntimeAssetExportTask> tasks) {
    public RuntimeAssetExportSetPlan {
        tasks = List.copyOf(tasks);

        var resourcePaths = new LinkedHashSet<String>();
        for (var task : tasks) {
            var resourcePath = task.asset().resourcePath();
            if (!resourcePaths.add(resourcePath)) {
                throw new IllegalArgumentException("Duplicate runtime asset resource path: " + resourcePath);
            }
        }
    }

    public static RuntimeAssetExportSetPlan of(List<RuntimeAssetExportTask> tasks) {
        return new RuntimeAssetExportSetPlan(tasks);
    }
}
