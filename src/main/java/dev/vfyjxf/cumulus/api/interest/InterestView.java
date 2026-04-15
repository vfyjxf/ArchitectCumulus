package dev.vfyjxf.cumulus.api.interest;

import dev.vfyjxf.cumulus.api.partition.SectionKey;
import net.minecraft.core.BlockPos;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 实例兴趣集快照。
 *
 * <p>兴趣集用于把“主动扫描”的范围收敛到与实例相关的 section/坐标/实体：
 * <ul>
 *     <li>{@link #sections()}：该实例关注的 sections（常用于 block/entity 轮询）</li>
 *     <li>{@link #watchPositions()}：少量关键坐标（可用于更细粒度触发）</li>
 *     <li>{@link #watchEntities()}：少量关键实体（可用于实体轮询）</li>
 * </ul>
 *
 * <p>ttl（可选）用于控制兴趣集膨胀：当 ttl 过期时，引擎/driver 可要求实例重新声明兴趣集。
 */
public record InterestView(
        Set<SectionKey> sections,
        Set<BlockPos> watchPositions,
        Set<UUID> watchEntities,
        Optional<Duration> ttl
) {
    public InterestView {
        sections = Set.copyOf(Objects.requireNonNull(sections, "sections"));
        watchPositions = Set.copyOf(Objects.requireNonNull(watchPositions, "watchPositions"));
        watchEntities = Set.copyOf(Objects.requireNonNull(watchEntities, "watchEntities"));
        ttl = Objects.requireNonNull(ttl, "ttl");
    }

    public static InterestView empty() {
        return new InterestView(Set.of(), Set.of(), Set.of(), Optional.empty());
    }
}
