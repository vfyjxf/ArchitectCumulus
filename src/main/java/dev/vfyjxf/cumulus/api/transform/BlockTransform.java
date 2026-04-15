package dev.vfyjxf.cumulus.api.transform;

import net.minecraft.core.BlockPos;

@FunctionalInterface
public interface BlockTransform {
    long toWorld(long anchorPackedPos, int localX, int localY, int localZ);

    default long toWorld(BlockPos anchorPos, int localX, int localY, int localZ) {
        return toWorld(anchorPos.asLong(), localX, localY, localZ);
    }

    static BlockTransform translateOnly() {
        return (anchorPackedPos, localX, localY, localZ) -> BlockPos.asLong(
                BlockPos.getX(anchorPackedPos) + localX,
                BlockPos.getY(anchorPackedPos) + localY,
                BlockPos.getZ(anchorPackedPos) + localZ
        );
    }
}
