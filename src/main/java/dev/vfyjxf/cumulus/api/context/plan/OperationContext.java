package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.context.CheckContext;
import java.util.Objects;

/**
 * Data access handed to a {@link ContextOperation} at execution time. Combines:
 * <ul>
 *   <li>{@link #matches()} — the matched points for this check candidate</li>
 *   <li>{@link #context()} — the live {@link CheckContext} to read derived values
 *       from (e.g. via {@code anyValue}) and to write computed values to
 *       ({@code putComputed})</li>
 * </ul>
 *
 * <p>Operations execute in declaration order, so a later operation can read a
 * value an earlier one wrote through {@link CheckContext#anyValue}.
 */
public final class OperationContext {

    private final MatchResults matches;
    private final CheckContext checkContext;

    public OperationContext(MatchResults matches, CheckContext checkContext) {
        this.matches = Objects.requireNonNull(matches, "matches must not be null");
        this.checkContext = Objects.requireNonNull(checkContext, "checkContext must not be null");
    }

    /** Query surface over the matched points. */
    public MatchResults matches() {
        return matches;
    }

    /** Live computation context: read derived/computed values, write computed values. */
    public CheckContext context() {
        return checkContext;
    }
}
