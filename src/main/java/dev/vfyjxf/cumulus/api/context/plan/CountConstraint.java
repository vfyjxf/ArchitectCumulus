package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.predicate.StructurePredicate;

import java.util.Objects;

/**
 * Constrains the count of matched points satisfying a {@link StructurePredicate}.
 * Verified by the executor after all matching (static + CheckPhase expansion)
 * is complete. If a constraint is violated, the structure status becomes
 * {@code invalid}.
 *
 * <pre>{@code
 * CountConstraint.atLeast(group("casing"), 8)     // ≥ 8 casing points
 * CountConstraint.atMost(group("port"), 4)         // ≤ 4 port points
 * CountConstraint.exactly(tag("cumulus:coil"), 4)   // exactly 4 coils
 * CountConstraint.range(group("casing"), 8, 26)     // 8~26 casing
 * }</pre>
 */
public record CountConstraint(
                StructurePredicate filter,
                int min,
                int max) {

    public CountConstraint {
        Objects.requireNonNull(filter, "filter must not be null");
        if (min < 0) {
            throw new IllegalArgumentException("CountConstraint min must be >= 0");
        }
        if (max < min) {
            throw new IllegalArgumentException("CountConstraint max must be >= min");
        }
    }

    /** At least {@code min} matching points required. */
    public static CountConstraint atLeast(StructurePredicate filter, int min) {
        return new CountConstraint(filter, min, Integer.MAX_VALUE);
    }

    /** At most {@code max} matching points allowed. */
    public static CountConstraint atMost(StructurePredicate filter, int max) {
        return new CountConstraint(filter, 0, max);
    }

    /** Exactly {@code count} matching points required. */
    public static CountConstraint exactly(StructurePredicate filter, int count) {
        return new CountConstraint(filter, count, count);
    }

    /** Between {@code min} and {@code max} (inclusive) matching points. */
    public static CountConstraint range(StructurePredicate filter, int min, int max) {
        return new CountConstraint(filter, min, max);
    }

    /** Check if {@code actualCount} satisfies this constraint. */
    public boolean isSatisfied(int actualCount) {
        return actualCount >= min && actualCount <= max;
    }
}
