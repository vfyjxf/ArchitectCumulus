package dev.vfyjxf.cumulus.api.interest;

import dev.vfyjxf.cumulus.api.partition.SectionKey;
import net.minecraft.core.BlockPos;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record InterestUpdate(
        Set<SectionKey> sectionsAdd,
        Set<SectionKey> sectionsRemove,
        Set<BlockPos> watchPositionsAdd,
        Set<BlockPos> watchPositionsRemove,
        Set<UUID> watchEntitiesAdd,
        Set<UUID> watchEntitiesRemove,
        Optional<Duration> ttl
) {
    public InterestUpdate {
        sectionsAdd = Set.copyOf(Objects.requireNonNull(sectionsAdd, "sectionsAdd"));
        sectionsRemove = Set.copyOf(Objects.requireNonNull(sectionsRemove, "sectionsRemove"));
        watchPositionsAdd = Set.copyOf(Objects.requireNonNull(watchPositionsAdd, "watchPositionsAdd"));
        watchPositionsRemove = Set.copyOf(Objects.requireNonNull(watchPositionsRemove, "watchPositionsRemove"));
        watchEntitiesAdd = Set.copyOf(Objects.requireNonNull(watchEntitiesAdd, "watchEntitiesAdd"));
        watchEntitiesRemove = Set.copyOf(Objects.requireNonNull(watchEntitiesRemove, "watchEntitiesRemove"));
        ttl = Objects.requireNonNull(ttl, "ttl");
    }

    public static InterestUpdate empty() {
        return new InterestUpdate(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Optional.empty());
    }
}
