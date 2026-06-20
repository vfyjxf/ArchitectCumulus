package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Read-only query surface over the set of structure points that matched during a
 * check. Handed to every {@link ContextOperation} via {@link OperationContext#matches()}.
 *
 * <p>Standard-library operations ({@code count}, {@code sum}, {@code bounds}, …)
 * are thin wrappers around these queries; custom operations may use them directly
 * or fall back to {@link #stream()} / {@link #all()} for arbitrary logic.
 */
public final class MatchResults {

    private final List<MatchedPoint> points;

    public MatchResults(List<MatchedPoint> points) {
        this.points = List.copyOf(points);
    }

    /** All matched points, in declaration order. */
    public List<MatchedPoint> all() {
        return points;
    }

    /** Stream over all matched points. */
    public Stream<MatchedPoint> stream() {
        return points.stream();
    }

    /** Points that belong to the named group. */
    public Stream<MatchedPoint> in(String group) {
        return points.stream().filter(p -> p.inGroup(group));
    }

    /** Points matching the given filter. */
    public Stream<MatchedPoint> matching(Predicate<MatchedPoint> filter) {
        return points.stream().filter(filter);
    }

    /** Count of points matching the filter. */
    public long count(Predicate<MatchedPoint> filter) {
        return points.stream().filter(filter).count();
    }

    /**
     * Sum of an {@code int} value extracted from each matching point.
     * Points whose selector returns {@code 0} contribute nothing.
     */
    public int sumInt(Predicate<MatchedPoint> filter, ToIntFunction<MatchedPoint> selector) {
        return points.stream().filter(filter).mapToInt(selector).sum();
    }

    /** First point matching the filter, in declaration order. */
    public Optional<MatchedPoint> first(Predicate<MatchedPoint> filter) {
        return points.stream().filter(filter).findFirst();
    }

    /**
     * Axis-aligned bounds of all points matching the filter, or empty if none match.
     */
    public Optional<Bounds3d> bounds(Predicate<MatchedPoint> filter) {
        RelativeBlockPos min = null;
        RelativeBlockPos max = null;
        for (var p : points) {
            if (!filter.test(p)) {
                continue;
            }
            var r = p.relativePos();
            if (min == null) {
                min = r;
                max = r;
            } else {
                min = RelativeBlockPos.of(Math.min(min.x(), r.x()), Math.min(min.y(), r.y()), Math.min(min.z(), r.z()));
                max = RelativeBlockPos.of(Math.max(max.x(), r.x()), Math.max(max.y(), r.y()), Math.max(max.z(), r.z()));
            }
        }
        return min == null ? Optional.empty() : Optional.of(new Bounds3d(min, max));
    }

    /**
     * Interior volume of a hollow shell described by the named group: treats the
     * group's bounding box as a one-block-thick shell and returns the volume of
     * the enclosed cavity, i.e. {@code (w-2)*(h-2)*(d-2)} clamped to be non-negative.
     *
     * <p>Returns empty if the group is absent. Returns {@code 0} if any dimension
     * is less than 3 (no enclosed cavity).
     */
    public Optional<Integer> interiorVolume(String shellGroup) {
        return bounds(p -> p.inGroup(shellGroup)).map(b -> {
            int w = Math.max(0, b.width() - 2);
            int h = Math.max(0, b.height() - 2);
            int d = Math.max(0, b.depth() - 2);
            return w * h * d;
        });
    }
}
