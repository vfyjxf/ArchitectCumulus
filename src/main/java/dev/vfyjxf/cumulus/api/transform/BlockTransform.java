package dev.vfyjxf.cumulus.api.transform;

import net.minecraft.core.BlockPos;

@FunctionalInterface
/**
 * 局部坐标到世界坐标的变换。
 *
 * <p>用于将结构部件的“相对坐标（local）”映射到“世界坐标（world）”。
 * v1 提供平移实现；旋转/镜像等可扩展实现该接口。
 */
public interface BlockTransform {
    /**
     * 将局部坐标映射到世界 packed blockPos。
     *
     * @param anchorPackedPos anchor 的 packed blockPos
     * @param localX          局部坐标 x
     * @param localY          局部坐标 y
     * @param localZ          局部坐标 z
     */
    long toWorld(long anchorPackedPos, int localX, int localY, int localZ);

    default long toWorld(BlockPos anchorPos, int localX, int localY, int localZ) {
        return toWorld(anchorPos.asLong(), localX, localY, localZ);
    }

    /**
     * 仅平移（world = anchor + local）。
     */
    static BlockTransform translateOnly() {
        return (anchorPackedPos, localX, localY, localZ) -> BlockPos.asLong(
                BlockPos.getX(anchorPackedPos) + localX,
                BlockPos.getY(anchorPackedPos) + localY,
                BlockPos.getZ(anchorPackedPos) + localZ
        );
    }
}
