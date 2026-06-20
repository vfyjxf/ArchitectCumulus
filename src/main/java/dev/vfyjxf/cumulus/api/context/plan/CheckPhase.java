package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import java.util.List;

/**
 * Validation hook inserted between static point matching and context-plan
 * operation execution. A check phase can:
 * <ul>
 *   <li>Read the currently-matched points via {@link PhaseContext#matches()}</li>
 *   <li>Read / write the computation context via {@link PhaseContext#context()}</li>
 *   <li>Return additional {@link StructurePointDefinition}s that the executor
 *       will match against the world snapshot before proceeding</li>
 * </ul>
 *
 * <p>This is the fundamental "dynamic structure" primitive: the shape of a
 * structure can adapt to what was actually found in the world. The high-level
 * convenience {@code CheckPhases.repeatRange(...)} uses this to implement
 * min/max axial repetition, but modders can implement fully custom logic.
 *
 * <pre>{@code
 * .checkPhase(ctx -> {
 *     var controller = ctx.matches().first(p -> p.id().value().equals("controller"));
 *     if (controller.isEmpty()) return List.of();
 *     // Decide how many casing layers to check based on world data...
 *     return extraPoints;
 * })
 * }</pre>
 *
 * <p>After the phase returns, the executor matches all returned points (resolve
 * target + evaluate predicates), adds their results to {@link MatchResults},
 * and then runs the {@code ContextPlan} operations — so operations can see
 * both static and dynamically-added points.
 */
@FunctionalInterface
public interface CheckPhase {

    /**
     * Evaluate this phase. Return a {@link PhaseResult} containing additional
     * points to match and/or count constraints to verify. Implementations may
     * also write computed values directly to the context.
     *
     * @param ctx access to matched points, live context, anchor, and transform
     * @return phase result (extra points + constraints); never null
     */
    PhaseResult evaluate(PhaseContext ctx);
}
