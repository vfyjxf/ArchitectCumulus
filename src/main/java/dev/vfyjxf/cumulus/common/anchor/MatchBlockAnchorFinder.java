package dev.vfyjxf.cumulus.common.anchor;

import dev.vfyjxf.cumulus.api.binding.plan.MatchSpec;
import dev.vfyjxf.cumulus.api.context.DiscoverContext;
import dev.vfyjxf.cumulus.api.context.ProbeContext;
import dev.vfyjxf.cumulus.api.partition.BlockStateView;
import dev.vfyjxf.cumulus.api.partition.LoadState;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.partition.SectionView;
import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.pass.ProbeResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 一个简单的 anchorFinder：按 {@link MatchSpec} 匹配“方块状态”作为 anchor。
 *
 * <p>实现策略：
 * <ul>
 *     <li>discover：扫描一个 section（16×16×16）内的全部方块</li>
 *     <li>probe：只检查 anchorPos 所在的方块</li>
 * </ul>
 *
 * <p>性能说明：discover 是 O(4096) 的全扫描，仅用于演示/测试；生产实现应考虑更高效的索引策略。
 */
public final class MatchBlockAnchorFinder implements AnchorFinder {
    private final MatchSpec match;

    public MatchBlockAnchorFinder(MatchSpec match) {
        this.match = Objects.requireNonNull(match, "match");
    }

    @Override
    public Stream<BlockPos> discover(DiscoverContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        SectionKey sectionKey = ctx.section();
        if (ctx.world().loadState(sectionKey) != LoadState.LOADED) {
            return Stream.empty();
        }
        SectionView section = ctx.world().section(sectionKey);

        int baseX = SectionPos.sectionToBlockCoord(sectionKey.chunkX());
        int baseY = SectionPos.sectionToBlockCoord(sectionKey.sectionY());
        int baseZ = SectionPos.sectionToBlockCoord(sectionKey.chunkZ());

        List<BlockPos> out = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // 按 16×16×16 扫描该 section。注意：这会触碰 4096 个位置，务必配合 budget。
        for (int dx = 0; dx < 16; dx++) {
            for (int dy = 0; dy < 16; dy++) {
                for (int dz = 0; dz < 16; dz++) {
                    if (!ctx.budget().tryConsume(1)) {
                        return out.stream();
                    }
                    int x = baseX + dx;
                    int y = baseY + dy;
                    int z = baseZ + dz;
                    long packed = BlockPos.asLong(x, y, z);
                    BlockStateView state = section.blockState(packed);
                    if (matchesBlock(match, state)) {
                        cursor.set(x, y, z);
                        out.add(cursor.immutable());
                    }
                }
            }
        }
        return out.stream();
    }

    @Override
    public ProbeResult probe(BlockPos anchorPos, ProbeContext ctx) {
        Objects.requireNonNull(anchorPos, "anchorPos");
        Objects.requireNonNull(ctx, "ctx");
        SectionKey sectionKey = SectionKey.from(anchorPos);
        if (ctx.world().loadState(sectionKey) != LoadState.LOADED) {
            return ProbeResult.UNLOADED;
        }
        if (!ctx.budget().tryConsume(1)) {
            return ProbeResult.PRESENT;
        }
        BlockStateView state = ctx.world().section(sectionKey).blockState(anchorPos.asLong());
        return matchesBlock(match, state) ? ProbeResult.PRESENT : ProbeResult.ABSENT;
    }

    private static boolean matchesBlock(MatchSpec match, BlockStateView state) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(state, "state");
        if (match instanceof MatchSpec.Any) {
            return true;
        }
        if (match instanceof MatchSpec.Id id) {
            return id.id().equals(state.id());
        }
        if (match instanceof MatchSpec.Tag tag) {
            return state.isInTag(tag.tag());
        }
        if (match instanceof MatchSpec.All all) {
            for (MatchSpec child : all.children()) {
                if (!matchesBlock(child, state)) {
                    return false;
                }
            }
            return true;
        }
        if (match instanceof MatchSpec.AnyOf anyOf) {
            for (MatchSpec child : anyOf.children()) {
                if (matchesBlock(child, state)) {
                    return true;
                }
            }
            return false;
        }
        if (match instanceof MatchSpec.Not not) {
            return !matchesBlock(not.child(), state);
        }
        return false;
    }
}
