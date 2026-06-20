package dev.vfyjxf.cumulus.api.migration;

import net.minecraft.resources.ResourceLocation;

public record StructureVersionDescriptor(
                int version,
                ResourceLocation definitionId) {
    public StructureVersionDescriptor {
        if (version < 1) {
            throw new IllegalArgumentException("Structure version must be positive");
        }
        if (definitionId == null) {
            throw new IllegalArgumentException("Version definition id must not be null");
        }
    }
}
