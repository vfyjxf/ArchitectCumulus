package dev.vfyjxf.cumulus.api.context.sources;

import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.CheckContext;
import java.util.Optional;

/**
 * Constant value source. Typed: the type parameter matches the value's type.
 */
public record LiteralSource<T>(T value) implements ContextSource<T> {

    public LiteralSource {
        if (value == null) {
            throw new IllegalArgumentException("LiteralSource value must not be null");
        }
    }

    @Override
    public Optional<T> resolve(CheckContext ctx) {
        return Optional.of(value);
    }
}
