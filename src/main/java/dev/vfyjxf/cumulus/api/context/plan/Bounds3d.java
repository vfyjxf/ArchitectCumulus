package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;

/**
 * Axis-aligned bounding box over the relative positions of a set of matched
 * structure points. Corners are inclusive.
 *
 * <p>Used by {@link MatchResults#bounds} and the {@code Operations.bounds} /
 * {@code Operations.interiorVolume} standard-library operations to describe the
 * extent of a matched group (e.g. the shell of a hollow box).
 *
 * @param min minimum corner (inclusive)
 * @param max maximum corner (inclusive)
 */
public record Bounds3d(RelativeBlockPos min, RelativeBlockPos max) {

    public Bounds3d {
        if (min == null || max == null) {
            throw new IllegalArgumentException("Bounds3d corners must not be null");
        }
    }

    public int width() {
        return max.x() - min.x() + 1;
    }

    public int height() {
        return max.y() - min.y() + 1;
    }

    public int depth() {
        return max.z() - min.z() + 1;
    }

    /** Total number of blocks spanned by the box (inclusive on every axis). */
    public int volume() {
        return width() * height() * depth();
    }
}
