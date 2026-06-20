package dev.vfyjxf.cumulus.api.context.plan;

/**
 * Open interface for one step in a {@link ContextPlan}. The check executor runs
 * each operation in declaration order, handing it an {@link OperationContext}
 * that exposes matched points and the live computation context.
 *
 * <p>Implementations write results to {@code ctx.context().putComputed(...)}.
 * Standard-library operations live in
 * {@link dev.vfyjxf.cumulus.api.context.plan.stdlib.Operations}; mods may provide
 * their own implementations and register them via
 * {@link ContextPlanBuilder#op(ContextOperation)}.
 *
 * <pre>{@code
 * public final class PlayerNearbyRecorder implements ContextOperation {
 *     public void execute(OperationContext ctx) {
 *         long count = ctx.matches().matching(p -> "minecraft:player".equals(p.value())).count();
 *         ctx.context().putComputed(PLAYER_COUNT, (int) count);
 *     }
 * }
 * }</pre>
 *
 * @see dev.vfyjxf.cumulus.api.context.plan.stdlib.Operations
 */
public interface ContextOperation {

    /**
     * Execute this operation against the given context. Implementations should
     * avoid throwing for "missing data" cases — write nothing (or a default)
     * and let downstream readers see an empty {@code anyValue}.
     */
    void execute(OperationContext ctx);
}
