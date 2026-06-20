package dev.vfyjxf.cumulus.api.context.sources;

import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import java.util.Optional;

/**
 * Resolve the absolute world position of a matched point, after the chosen
 * transform candidate has been applied. Equivalent to the legacy
 * {@code "controller.position"} source string.
 */
public record CapturePositionSource(StructurePointId point) implements ContextSource<String> {

    public CapturePositionSource {
        if (point == null) {
            throw new IllegalArgumentException("CapturePositionSource point must not be null");
        }
    }

    @Override
    public Optional<String> resolve(CheckContext ctx) {
        return ctx.resolvedTarget(point).map(CheckContext.ResolvedTarget::absolutePosition);
    }
}
