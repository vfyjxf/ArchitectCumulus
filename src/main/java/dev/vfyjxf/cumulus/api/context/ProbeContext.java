package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.engine.Budget;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;

import java.util.Objects;

/**
 * anchor probe 上下文。
 */
public record ProbeContext(PartitionAccess world, Budget budget) {
    public ProbeContext {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(budget, "budget");
    }
}
