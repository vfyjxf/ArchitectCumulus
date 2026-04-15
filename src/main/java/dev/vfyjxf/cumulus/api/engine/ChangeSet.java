package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.partition.SectionKey;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 外部变化提示（通常来自事件流）。
 *
 * <p>v1 的最小模型是 dirty sections：只要知道“哪些 {@link SectionKey} 发生过变化”，引擎就能增量收敛。
 * 其他字段（dirtyAnchors/touchedEntities）是可选 hint，用于更精确地定位受影响实例。
 *
 * <p>注意：ChangeSet 不等价于“必须加载哪些区块”。引擎遇到未加载数据时依旧使用 {@code DataRequest} 表达缺口。
 */
public record ChangeSet(
        Set<SectionKey> dirtySections,
        Set<BlockPos> dirtyAnchors,
        Set<UUID> touchedEntities,
        ChangeCause cause
) {
    public ChangeSet {
        dirtySections = Set.copyOf(Objects.requireNonNull(dirtySections, "dirtySections"));
        dirtyAnchors = Set.copyOf(Objects.requireNonNull(dirtyAnchors, "dirtyAnchors"));
        touchedEntities = Set.copyOf(Objects.requireNonNull(touchedEntities, "touchedEntities"));
        Objects.requireNonNull(cause, "cause");
    }

    public static ChangeSet empty() {
        return new ChangeSet(Set.of(), Set.of(), Set.of(), ChangeCause.UNKNOWN);
    }

    /**
     * 创建一个 builder。
     */
    public static Builder builder(ChangeCause cause) {
        return new Builder(cause);
    }

    public static final class Builder {
        private final Set<SectionKey> dirtySections = new HashSet<>();
        private final Set<BlockPos> dirtyAnchors = new HashSet<>();
        private final Set<UUID> touchedEntities = new HashSet<>();
        private final ChangeCause cause;

        private Builder(ChangeCause cause) {
            this.cause = Objects.requireNonNull(cause, "cause");
        }

        /**
         * 标记一个 dirty section。
         */
        public Builder dirtySection(SectionKey key) {
            dirtySections.add(Objects.requireNonNull(key, "key"));
            return this;
        }

        /**
         * 标记一个可能受影响的 anchor。
         */
        public Builder dirtyAnchor(BlockPos anchorPos) {
            dirtyAnchors.add(Objects.requireNonNull(anchorPos, "anchorPos"));
            return this;
        }

        /**
         * 标记一个被触碰的实体（可选 hint）。
         */
        public Builder touchedEntity(UUID entityId) {
            touchedEntities.add(Objects.requireNonNull(entityId, "entityId"));
            return this;
        }

        /**
         * 构建 ChangeSet。
         */
        public ChangeSet build() {
            return new ChangeSet(dirtySections, dirtyAnchors, touchedEntities, cause);
        }
    }
}
