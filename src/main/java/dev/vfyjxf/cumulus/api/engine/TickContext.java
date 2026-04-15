package dev.vfyjxf.cumulus.api.engine;

import java.util.Objects;

public record TickContext(long tick, Budget budget) {
    public TickContext {
        Objects.requireNonNull(budget, "budget");
    }
}
