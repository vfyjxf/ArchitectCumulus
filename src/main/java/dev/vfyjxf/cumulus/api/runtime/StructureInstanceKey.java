package dev.vfyjxf.cumulus.api.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record StructureInstanceKey(ResourceLocation structureId, BlockPos anchor) {
    public StructureInstanceKey {
        if (structureId == null) {
            throw new IllegalArgumentException("Structure id must not be null");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("Anchor must not be null");
        }
    }
}
