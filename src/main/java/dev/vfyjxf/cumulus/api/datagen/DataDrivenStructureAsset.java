package dev.vfyjxf.cumulus.api.datagen;

import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import net.minecraft.resources.ResourceLocation;

public record DataDrivenStructureAsset(
        ResourceLocation runtimeAssetId,
        int schemaVersion,
        ResourceLocation definitionId,
        StructureDefinition definition,
        boolean debugAsset) {
    public DataDrivenStructureAsset {
        if (runtimeAssetId == null) {
            throw new IllegalArgumentException("Runtime asset id must not be null");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Runtime asset schema version must be positive");
        }
        if (definitionId == null) {
            throw new IllegalArgumentException("Definition id must not be null");
        }
        if (definition != null && !definition.id().equals(definitionId)) {
            throw new IllegalArgumentException("Embedded definition id must match asset definition id");
        }
    }

    public DataDrivenStructureAsset(
            ResourceLocation runtimeAssetId,
            int schemaVersion,
            ResourceLocation definitionId,
            boolean debugAsset) {
        this(runtimeAssetId, schemaVersion, definitionId, null, debugAsset);
    }

    public static DataDrivenStructureAsset fromDefinition(
            ResourceLocation runtimeAssetId,
            int schemaVersion,
            StructureDefinition definition,
            boolean debugAsset) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        return new DataDrivenStructureAsset(runtimeAssetId, schemaVersion, definition.id(), definition, debugAsset);
    }

    public String resourcePath() {
        return "data/" + runtimeAssetId.getNamespace()
                + "/cumulus/structures/"
                + runtimeAssetId.getPath()
                + ".json";
    }
}
