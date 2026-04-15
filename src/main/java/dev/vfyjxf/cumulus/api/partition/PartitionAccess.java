package dev.vfyjxf.cumulus.api.partition;

/**
 * 世界数据读取抽象（按 {@link SectionKey} 分块）。
 *
 * <p>该接口只提供“只读访问”。当 section 未加载时，必须能通过 {@link LoadState#MISSING} 表达缺失。
 * 上层 pass 应据此返回 {@code INCOMPLETE + DataRequest}，而不是在这里强制加载。
 *
 * <p>注意：{@link #section(SectionKey)} 在 missing 时仍然可能返回一个“MissingSectionView”风格的对象，
 * 以避免空指针；调用方仍需通过 {@link #loadState(SectionKey)} 或 {@link SectionView#loadState()} 判断是否可用。
 */
public interface PartitionAccess {
    /**
     * 查询某个 section 是否已加载。
     */
    LoadState loadState(SectionKey key);

    /**
     * 获取某个 section 的只读视图。
     *
     * <p>在 {@link LoadState#MISSING} 时，建议返回一个“空/缺失视图”，而不是抛异常。
     */
    SectionView section(SectionKey key);

    /**
     * 枚举当前已加载的 sections（可选）。
     *
     * <p>用于 driver 的 loaded-only 扫描策略；默认实现返回空集合，表示“无法枚举”。
     */
    default Iterable<SectionKey> loadedSections() {
        return java.util.List.of();
    }
}
