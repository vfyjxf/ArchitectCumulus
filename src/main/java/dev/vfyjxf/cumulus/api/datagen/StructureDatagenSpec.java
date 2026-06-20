package dev.vfyjxf.cumulus.api.datagen;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record StructureDatagenSpec(
                String namespace,
                String path,
                Map<ResourceLocation, DataDrivenStructureAsset> assets) {
    public StructureDatagenSpec {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("Datagen namespace must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Datagen path must not be blank");
        }
        assets = Map.copyOf(assets);
    }

    public String resourcePath() {
        return "data/" + namespace + "/cumulus/structures/" + path + ".json";
    }
}
