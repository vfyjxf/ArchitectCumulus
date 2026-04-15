package dev.vfyjxf.cumulus.api.partition;

import java.util.Optional;

public interface SectionView {
    SectionKey key();

    LoadState loadState();

    BlockStateView blockState(long packedBlockPos);

    Optional<BlockEntityView> blockEntity(long packedBlockPos);

    Iterable<EntityView> entities();
}
