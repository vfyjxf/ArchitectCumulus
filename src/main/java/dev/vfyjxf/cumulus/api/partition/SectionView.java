package dev.vfyjxf.cumulus.api.partition;

import java.util.Optional;

/**
 * 一个 section 的只读视图。
 *
 * <p>实现可以基于真实世界（Level/Chunk）或缓存（snapshot/cache）提供读取能力。
 * 该接口刻意保持低抽象成本：主要面向结构检查需要的“方块/方块实体/实体”三类信息。
 */
public interface SectionView {
    /**
     * section 键。
     */
    SectionKey key();

    /**
     * 加载状态。
     */
    LoadState loadState();

    /**
     * 获取指定 blockPos（packed long）的方块状态视图。
     *
     * <p>当该位置不存在/不可用时，实现可返回一个 sentinel（例如 “missing” block），
     * 上层用 matcher 进行判断即可。
     */
    BlockStateView blockState(long packedBlockPos);

    /**
     * 获取指定位置的方块实体视图（如果存在）。
     */
    Optional<BlockEntityView> blockEntity(long packedBlockPos);

    /**
     * 枚举该 section 内的实体视图。
     *
     * <p>实现可返回快照列表或惰性迭代器；调用方应配合预算避免全量扫描过重。
     */
    Iterable<EntityView> entities();
}
