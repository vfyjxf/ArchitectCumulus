package dev.vfyjxf.cumulus.api.engine;

import java.util.Objects;

/**
 * 引擎 tick 的上下文。
 *
 * @param tick   当前 tick 计数（由宿主提供，用于驱动 driver 与节流/轮询逻辑）
 * @param budget 本 tick 的总预算
 */
public record TickContext(long tick, Budget budget) {
    public TickContext {
        Objects.requireNonNull(budget, "budget");
    }
}
