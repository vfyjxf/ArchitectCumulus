package dev.vfyjxf.cumulus.api.context;

import java.util.Optional;

/**
 * Open interface: produces a typed value to be stored under a {@code ContextKey<T>}
 * in {@code RuntimeStructureContext}.
 *
 * <p>The type parameter {@code T} declares the value type this source produces.
 * Built-in sources:
 * <ul>
 *   <li>{@link dev.vfyjxf.cumulus.api.context.sources.CapturePropertySource} implements {@code ContextSource<String>}</li>
 *   <li>{@link dev.vfyjxf.cumulus.api.context.sources.CaptureValueSource} implements {@code ContextSource<String>}</li>
 *   <li>{@link dev.vfyjxf.cumulus.api.context.sources.CapturePositionSource} implements {@code ContextSource<String>}</li>
 *   <li>{@link dev.vfyjxf.cumulus.api.context.sources.ContextRefSource ContextRefSource&lt;T&gt;} implements {@code ContextSource<T>}</li>
 *   <li>{@link dev.vfyjxf.cumulus.api.context.sources.LiteralSource LiteralSource&lt;T&gt;} implements {@code ContextSource<T>}</li>
 * </ul>
 *
 * <p>Extensions: implement {@code ContextSource<T>} in a final record/class. The
 * check executor calls {@link #resolve} and puts the result into the context map
 * under the rule's target key — the {@code ContextKey<T>} on the put side validates
 * that the source's declared type matches.
 *
 * @param <T> the value type this source produces
 */
public interface ContextSource<T> {
    /**
     * Compute the value from the in-progress {@link CheckContext}, or empty if
     * it cannot be resolved (missing point, missing property, etc.).
     * Empty is not an error — the rule is silently skipped.
     */
    Optional<T> resolve(CheckContext ctx);
}
