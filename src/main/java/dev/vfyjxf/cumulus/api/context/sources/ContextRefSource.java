package dev.vfyjxf.cumulus.api.context.sources;

import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.types.ContextKey;

import java.util.Optional;

/**
 * Copy the value of an already-resolved context key into a new key.
 * Typed: the type parameter matches the referenced {@link ContextKey}'s value type.
 */
public record ContextRefSource<T>(ContextKey<T> key) implements ContextSource<T> {

    public ContextRefSource {
        if (key == null) {
            throw new IllegalArgumentException("ContextRefSource key must not be null");
        }
    }

    @Override
    public Optional<T> resolve(CheckContext ctx) {
        return ctx.derivedValue(key);
    }
}
