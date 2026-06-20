package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.types.ContextKey;
import java.util.Objects;

/**
 * Author rule that derives one {@code RuntimeStructureContext} entry from
 * matched structure data.
 *
 * <p><b>Compile-time type safety:</b> The type parameter {@code T} enforces that
 * the source produces the same type the key expects:
 * <pre>{@code
 * // ✓ compiles: both are String
 * .context(ContextRuleDescriptor.derive(
 *     ContextKey.string("controller_id"),
 *     ContextSources.capture("controller").value()))
 * }</pre>
 *
 * @param <T> the value type — shared by key and source
 */
public record ContextRuleDescriptor<T>(ContextKey<T> targetKey, ContextSource<T> source) {

    public ContextRuleDescriptor {
        Objects.requireNonNull(targetKey, "Context rule target key must not be null");
        Objects.requireNonNull(source, "Context rule source must not be null");
    }

    /**
     * Factory: the compiler verifies {@code T} is consistent between key and source.
     */
    public static <T> ContextRuleDescriptor<T> derive(ContextKey<T> targetKey, ContextSource<T> source) {
        return new ContextRuleDescriptor<>(targetKey, source);
    }
}
