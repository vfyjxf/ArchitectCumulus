package dev.vfyjxf.cumulus.api.context.plan;

/**
 * Unified evaluation hook — runs after static point matching, can:
 * <ul>
 *   <li>Read matched points via {@link PhaseContext#matches()}</li>
 *   <li>Read / write the computation context via {@link PhaseContext#context()}</li>
 *   <li>Return additional points to match + count constraints via {@link PhaseResult}</li>
 * </ul>
 *
 * <p>This merges the former {@code CheckPhase} (dynamic point expansion) and
 * {@link EvaluationPhase} (value computation) into a single concept: a phase
 * of evaluation that has full access to matched data and can influence both
 * the point set (via extra points) and the context values (via direct writes).
 *
 * <pre>{@code
 * .checkPhase(ctx -> {
 *     var controller = ctx.matches().first(p -> p.id().value().equals("controller"));
 *     if (controller.isEmpty()) return PhaseResult.empty();
 *
 *     // Write computed values directly
 *     ctx.context().put(ENERGY, controller.get().intProperty("energy").orElse(0));
 *
 *     // Return extra points + constraints
 *     return PhaseResult.of(extraPoints, List.of(
 *         CountConstraint.atLeast(group("casing"), 8)));
 * })
 * }</pre>
 *
 * @see CheckPhase alias kept for backward compatibility
 */
@FunctionalInterface
public interface EvaluationPhase {

    /**
     * Evaluate this phase. Return a {@link PhaseResult} containing additional
     * points to match and/or count constraints. May also write values directly
     * to the context.
     *
     * @param ctx access to matched points, mutable context, anchor, and transform
     * @return phase result (extra points + constraints); never null
     */
    PhaseResult evaluate(PhaseContext ctx);
}
