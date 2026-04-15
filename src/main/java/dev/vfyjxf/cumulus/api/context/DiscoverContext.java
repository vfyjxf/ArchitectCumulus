package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.engine.Budget;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;
import dev.vfyjxf.cumulus.api.partition.SectionKey;

import java.util.Objects;

public record DiscoverContext(PartitionAccess world, SectionKey section, Budget budget) {
    public DiscoverContext {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(budget, "budget");
    }
}
