package dev.vfyjxf.cumulus.api.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record InstanceKey(ResourceLocation schemaId, BlockPos anchorPos) {
    public InstanceKey {
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(anchorPos, "anchorPos");
    }
}
