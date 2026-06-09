package dev.vfyjxf.cumulus.api.partition;

import net.minecraft.core.BlockPos;

public record SectionKey(int x, int y, int z) {
    public static SectionKey fromBlock(BlockPos pos) {
        if (pos == null) {
            throw new IllegalArgumentException("Block position must not be null");
        }
        return new SectionKey(
                Math.floorDiv(pos.getX(), 16),
                Math.floorDiv(pos.getY(), 16),
                Math.floorDiv(pos.getZ(), 16));
    }
}
