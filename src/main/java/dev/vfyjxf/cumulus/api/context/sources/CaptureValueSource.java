package dev.vfyjxf.cumulus.api.context.sources;

import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import java.util.Optional;

/**
 * Resolve the primary captured value of a matched point (the snapshot's
 * {@code value()} — typically a block id, fluid id, or entity type id).
 * Equivalent to the legacy {@code "controller.value"} source string.
 */
public record CaptureValueSource(StructurePointId point) implements ContextSource<String> {

    public CaptureValueSource {
        if (point == null) {
            throw new IllegalArgumentException("CaptureValueSource point must not be null");
        }
    }

    @Override
    public Optional<String> resolve(CheckContext ctx) {
        return ctx.resolvedTarget(point).map(CheckContext.ResolvedTarget::value);
    }
}
