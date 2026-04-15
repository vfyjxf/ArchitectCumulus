package dev.vfyjxf.cumulus.common.driver;

import dev.vfyjxf.cumulus.api.driver.DriverContext;
import dev.vfyjxf.cumulus.api.driver.DriverOutput;
import dev.vfyjxf.cumulus.api.driver.DriverStrategy;
import dev.vfyjxf.cumulus.api.engine.ChangeCause;
import dev.vfyjxf.cumulus.api.partition.SectionKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * loaded-only 对账扫描：定期枚举 {@link dev.vfyjxf.cumulus.api.partition.PartitionAccess#loadedSections()}，
 * 并以“每 tick 固定数量”输出 dirty sections。
 *
 * <p>用途：
 * <ul>
 *     <li>没有事件流时：让引擎最终收敛（低频全量巡检）</li>
 *     <li>事件不完整时：兜底修正漏掉的变化</li>
 * </ul>
 *
 * <p>注意：该策略只扫描“已加载范围内”，不会主动加载区块。
 */
public final class LoadedReconcileScanDriver implements DriverStrategy {
    private final int sectionsPerTick;
    private final int refreshIntervalTicks;

    private long lastRefreshTick = Long.MIN_VALUE;
    private List<SectionKey> snapshot = List.of();
    private int cursor = 0;

    public LoadedReconcileScanDriver(int sectionsPerTick, int refreshIntervalTicks) {
        if (sectionsPerTick <= 0) {
            throw new IllegalArgumentException("sectionsPerTick must be > 0");
        }
        if (refreshIntervalTicks <= 0) {
            throw new IllegalArgumentException("refreshIntervalTicks must be > 0");
        }
        this.sectionsPerTick = sectionsPerTick;
        this.refreshIntervalTicks = refreshIntervalTicks;
    }

    public static LoadedReconcileScanDriver defaultLoadedOnly() {
        return new LoadedReconcileScanDriver(64, 20);
    }

    @Override
    public DriverOutput run(DriverContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        long tick = ctx.tick();

        // 周期性刷新 loadedSections 快照，避免每 tick 枚举导致开销过大。
        if (tick - lastRefreshTick >= refreshIntervalTicks || cursor >= snapshot.size()) {
            List<SectionKey> next = new ArrayList<>();
            for (SectionKey key : ctx.world().loadedSections()) {
                next.add(key);
                if (!ctx.budget().tryConsume(1)) {
                    break;
                }
            }
            snapshot = next;
            lastRefreshTick = tick;
            cursor = Math.min(cursor, Math.max(0, snapshot.size() - 1));
        }

        if (snapshot.isEmpty()) {
            return DriverOutput.empty(ChangeCause.SCAN);
        }

        Set<SectionKey> dirty = new HashSet<>();
        // 每 tick 取固定数量推进游标，形成轮询。
        for (int i = 0; i < sectionsPerTick; i++) {
            if (!ctx.budget().tryConsume(1)) {
                break;
            }
            if (snapshot.isEmpty()) {
                break;
            }
            if (cursor >= snapshot.size()) {
                cursor = 0;
            }
            dirty.add(snapshot.get(cursor++));
        }

        return new DriverOutput(dirty, Set.of(), List.of(), ChangeCause.SCAN);
    }
}
