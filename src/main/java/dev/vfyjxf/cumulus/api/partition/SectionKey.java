package dev.vfyjxf.cumulus.api.partition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Objects;

/**
 * 分块键：Minecraft section（16×16×16）。
 *
 * <p>section 维度：
 * <ul>
 *     <li>{@link #chunkX()} / {@link #chunkZ()}：chunk 坐标（16×16）</li>
 *     <li>{@link #sectionY()}：垂直方向 section 坐标（每 16 格一个 section）</li>
 * </ul>
 *
 * <p>该键用于：
 * <ul>
 *     <li>增量 dirty 粒度（ChangeSet/DriverOutput）</li>
 *     <li>缺数据请求粒度（DataRequest.section）</li>
 *     <li>兴趣集粒度（InterestView.sections）</li>
 * </ul>
 */
public record SectionKey(int chunkX, int sectionY, int chunkZ) {
    public SectionKey {
        // record canonical constructor exists for future validation hooks
    }

    /**
     * 从世界坐标推导所属 section。
     */
    public static SectionKey from(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        return new SectionKey(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
        );
    }

    public ChunkPos chunkPos() {
        return new ChunkPos(chunkX, chunkZ);
    }
}
