package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.DiscoverContext;
import dev.vfyjxf.cumulus.api.context.ProbeContext;
import net.minecraft.core.BlockPos;

import java.util.stream.Stream;

/**
 * 锚点发现/探测器。
 *
 * <p>职责：
 * <ul>
 *     <li>{@link #discover(DiscoverContext)}：在一个已加载 section 内发现可能的 anchorPos</li>
 *     <li>{@link #probe(BlockPos, ProbeContext)}：快速判断某个 anchorPos 是否仍然成立</li>
 * </ul>
 *
 * <p>重要约束：v1 设计中不强制加载区块，因此 discover/probe 在未加载时应返回空/UNLOADED，
 * 而不是主动请求加载。
 */
public interface AnchorFinder {
    /**
     * 在 {@link DiscoverContext#section()} 范围内发现 anchor。
     *
     * <p>建议只扫描已加载 section（由上层控制 dirtySections + loadState）。
     */
    Stream<BlockPos> discover(DiscoverContext ctx);

    /**
     * 探测某个 anchor 是否存在。
     *
     * @return PRESENT 表示存在；ABSENT 表示不再存在；UNLOADED 表示因缺数据无法判断
     */
    ProbeResult probe(BlockPos anchorPos, ProbeContext ctx);
}
