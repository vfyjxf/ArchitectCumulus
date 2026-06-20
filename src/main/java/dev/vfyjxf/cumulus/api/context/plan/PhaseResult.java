package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import java.util.List;

/**
 * Result of a {@link CheckPhase#evaluate(PhaseContext)} call. Combines:
 * <ul>
 *   <li>{@link #extraPoints} — additional points to match against the world</li>
 *   <li>{@link #constraints} — count constraints verified after all matching</li>
 * </ul>
 *
 * <p>Factory methods:
 * <pre>{@code
 * PhaseResult.empty()                               // no expansion, no constraints
 * PhaseResult.of(points)                            // extra points, no constraints
 * PhaseResult.of(points, constraints)               // extra points + constraints
 * PhaseResult.constraints(constraints)              // no extra points, just constraints
 * }</pre>
 */
public record PhaseResult(
        List<StructurePointDefinition> extraPoints,
        List<CountConstraint> constraints) {

    public PhaseResult {
        extraPoints = extraPoints == null ? List.of() : List.copyOf(extraPoints);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
    }

    /** No expansion, no constraints. */
    public static PhaseResult empty() {
        return new PhaseResult(List.of(), List.of());
    }

    /** Extra points only, no constraints. */
    public static PhaseResult of(List<StructurePointDefinition> extraPoints) {
        return new PhaseResult(extraPoints, List.of());
    }

    /** Extra points + count constraints. */
    public static PhaseResult of(List<StructurePointDefinition> extraPoints, List<CountConstraint> constraints) {
        return new PhaseResult(extraPoints, constraints);
    }

    /** Constraints only, no extra points. */
    public static PhaseResult constraints(List<CountConstraint> constraints) {
        return new PhaseResult(List.of(), constraints);
    }
}
