package dev.vfyjxf.cumulus.api.ownership;

import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;

import net.minecraft.resources.ResourceLocation;

public record PartClaim(
                ResourceLocation partId,
                StructureInstanceKey owner,
                ClaimMode mode) {
    public PartClaim {
        if (partId == null) {
            throw new IllegalArgumentException("Part id must not be null");
        }
        if (owner == null) {
            throw new IllegalArgumentException("Claim owner must not be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Claim mode must not be null");
        }
    }
}
