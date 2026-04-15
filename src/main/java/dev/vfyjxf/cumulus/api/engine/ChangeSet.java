package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.partition.SectionKey;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

        public Builder dirtySection(SectionKey key) {
            dirtySections.add(Objects.requireNonNull(key, "key"));
            return this;
        }

        public Builder dirtyAnchor(BlockPos anchorPos) {
            dirtyAnchors.add(Objects.requireNonNull(anchorPos, "anchorPos"));
            return this;
        }

        public Builder touchedEntity(UUID entityId) {
            touchedEntities.add(Objects.requireNonNull(entityId, "entityId"));
            return this;
        }

        public ChangeSet build() {
            return new ChangeSet(dirtySections, dirtyAnchors, touchedEntities, cause);
        }
    }
}
