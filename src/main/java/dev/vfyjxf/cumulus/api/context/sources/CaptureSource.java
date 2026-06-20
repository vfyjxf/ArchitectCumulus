package dev.vfyjxf.cumulus.api.context.sources;

import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;

import java.util.Objects;
import java.util.Optional;

/**
 * Unified capture source — reads a field from a matched point's
 * {@link CheckContext.ResolvedTarget}. Replaces the former
 * {@code CaptureValueSource}, {@code CapturePositionSource}, and
 * {@code CapturePropertySource} with a single type discriminated by
 * {@link CaptureField}.
 */
public record CaptureSource(StructurePointId point, CaptureField field) implements ContextSource<String> {

    public CaptureSource {
        Objects.requireNonNull(point, "point must not be null");
        Objects.requireNonNull(field, "field must not be null");
    }

    @Override
    public Optional<String> resolve(CheckContext ctx) {
        return ctx.resolvedTarget(point).map(field::extract);
    }
}
