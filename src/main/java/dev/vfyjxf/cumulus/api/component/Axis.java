package dev.vfyjxf.cumulus.api.component;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;

/**
 * Coordinate axis, used by {@link dev.vfyjxf.cumulus.api.context.plan.stdlib.CheckPhases#repeatRange}
 * and other axial-repeat operations.
 */
public enum Axis {
    x,
    y,
    z;

    /** Shift a relative position by {@code amount} along this axis. */
    public RelativeBlockPos shift(RelativeBlockPos pos, int amount) {
        return switch (this) {
            case x -> RelativeBlockPos.of(pos.x() + amount, pos.y(), pos.z());
            case y -> RelativeBlockPos.of(pos.x(), pos.y() + amount, pos.z());
            case z -> RelativeBlockPos.of(pos.x(), pos.y(), pos.z() + amount);
        };
    }
}
