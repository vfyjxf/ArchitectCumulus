package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import java.util.List;

/**
 * Validation hook inserted between static point matching and context-plan
 * operation execution. Now unified with {@link EvaluationPhase} — this
 * interface extends it for backward compatibility. New code should use
 * {@code EvaluationPhase} or {@code CheckPhase} interchangeably.
 */
@FunctionalInterface
public interface CheckPhase extends EvaluationPhase {

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
