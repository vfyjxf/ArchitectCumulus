package dev.vfyjxf.cumulus.api.interest;

import dev.vfyjxf.cumulus.api.partition.SectionKey;
import net.minecraft.core.BlockPos;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

