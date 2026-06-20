package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Data access handed to a {@link CheckPhase} at evaluation time. Combines:
 * <ul>
 *   <li>{@link #matches()} — the points matched so far (static entries only)</li>
 *   <li>{@link #context()} — the live {@link CheckContext} (read derived/computed/captures, write computed)</li>
 *   <li>{@link #anchor()} — the absolute world position of the structure anchor</li>
 *   <li>{@link #transform()} — the transform candidate currently being evaluated</li>
 * </ul>
 *
 * <p>The phase can read the current state, write computed values for downstream
 * consumption, and return additional points to match. After those points are
 * matched, the {@code ContextPlan} operations run and can see the full set.
 */
public final class PhaseContext {

    private final MatchResults matches;
    private final CheckContext checkContext;
    private final BlockPos anchor;
    private final TransformDescriptor transform;

    public PhaseContext(MatchResults matches, CheckContext checkContext, BlockPos anchor, TransformDescriptor transform) {
        this.matches = Objects.requireNonNull(matches, "matches must not be null");
        this.checkContext = Objects.requireNonNull(checkContext, "checkContext must not be null");
        this.anchor = Objects.requireNonNull(anchor, "anchor must not be null");
        this.transform = Objects.requireNonNull(transform, "transform must not be null");
    }

    /** Query surface over the points matched so far. */
    public MatchResults matches() {
        return matches;
    }

    /** Live computation context: read derived/computed/captures, write computed values. */
    public CheckContext context() {
        return checkContext;
    }

    /** The absolute world position of the structure anchor for this check candidate. */
    public BlockPos anchor() {
        return anchor;
    }

    /** The transform candidate currently being evaluated (for computing dynamic point positions). */
    public TransformDescriptor transform() {
        return transform;
    }
}
