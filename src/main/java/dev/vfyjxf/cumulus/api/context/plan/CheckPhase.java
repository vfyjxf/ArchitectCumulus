package dev.vfyjxf.cumulus.api.context.plan;

/**
 * Validation hook inserted between static point matching and context-plan
 * operation execution. Now unified with {@link EvaluationPhase} — this
 * interface extends it for backward compatibility. New code should use
 * {@code EvaluationPhase} or {@code CheckPhase} interchangeably.
 */
@FunctionalInterface
public interface CheckPhase extends EvaluationPhase {

}
