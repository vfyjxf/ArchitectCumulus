package dev.vfyjxf.cumulus.api.partition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Objects;

public record SectionKey(int chunkX, int sectionY, int chunkZ) {
    public SectionKey {
        // record canonical constructor exists for future validation hooks
    }

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
