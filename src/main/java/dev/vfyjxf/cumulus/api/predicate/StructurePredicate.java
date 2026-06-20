package dev.vfyjxf.cumulus.api.predicate;

import dev.vfyjxf.cumulus.api.context.plan.MatchedPoint;

import java.util.function.Predicate;

/**
 * Strongly-typed predicate that tests a {@link MatchedPoint} — the
 * post-match view of a structure point. Extends {@link Predicate} so it
 * interoperates seamlessly with {@link
 * dev.vfyjxf.cumulus.api.context.plan.MatchResults}'s filtering methods.
 *
 * <p>Supports full boolean composition: {@link #and}, {@link #or}, {@link #negate}.
 */
public interface StructurePredicate extends Predicate<MatchedPoint> {

    @Override
    boolean test(MatchedPoint point);

    @Override
    default StructurePredicate and(Predicate<? super MatchedPoint> other) {
        return point -> this.test(point) && other.test(point);
    }

    @Override
    default StructurePredicate or(Predicate<? super MatchedPoint> other) {
        return point -> this.test(point) || other.test(point);
    }

    @Override
    default StructurePredicate negate() {
        return point -> !this.test(point);
    }
}
