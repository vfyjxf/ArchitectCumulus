package dev.vfyjxf.cumulus.impl.codec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class RuntimeAssetRepository {
    private final Map<ResourceLocation, LoadedRuntimeAsset> assetsById = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<LoadedRuntimeAsset>> assetsByDefinition = new LinkedHashMap<>();
    private RuntimeAssetLoadResult lastResult;

    public synchronized void apply(RuntimeAssetLoadResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Runtime asset load result must not be null");
        }
        lastResult = result;
        if (!result.valid()) {
            return;
        }

        var nextById = new LinkedHashMap<ResourceLocation, LoadedRuntimeAsset>();
        var nextByDefinition = new LinkedHashMap<ResourceLocation, List<LoadedRuntimeAsset>>();
        for (var asset : result.assets()) {
            nextById.put(asset.id(), asset);
            nextByDefinition.computeIfAbsent(asset.definitionId(), ignored -> new ArrayList<>()).add(asset);
        }

        assetsById.clear();
        assetsById.putAll(nextById);
        assetsByDefinition.clear();
        nextByDefinition.forEach((definition, assets) ->
                assetsByDefinition.put(definition, List.copyOf(assets)));
    }

    public synchronized List<LoadedRuntimeAsset> assets() {
        return List.copyOf(assetsById.values());
    }

    public synchronized Optional<LoadedRuntimeAsset> byId(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Runtime asset id must not be null");
        }
        return Optional.ofNullable(assetsById.get(id));
    }

    public synchronized List<LoadedRuntimeAsset> byDefinition(ResourceLocation definitionId) {
        if (definitionId == null) {
            throw new IllegalArgumentException("Definition id must not be null");
        }
        return assetsByDefinition.getOrDefault(definitionId, List.of());
    }

    public synchronized Optional<RuntimeAssetLoadResult> lastResult() {
        return Optional.ofNullable(lastResult);
    }
}
