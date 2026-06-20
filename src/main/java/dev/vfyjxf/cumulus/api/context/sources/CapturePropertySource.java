package dev.vfyjxf.cumulus.api.context.sources;

import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import java.util.Optional;

/**
 * Resolve a block-state / capability / NBT property of a matched point.
 * Equivalent to the legacy {@code "controller.facing"} source string.
 */
public record CapturePropertySource(StructurePointId point, String property) implements ContextSource<String> {

    public CapturePropertySource {
        if (point == null) {
            throw new IllegalArgumentException("CapturePropertySource point must not be null");
        }
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException("CapturePropertySource property must not be blank");
        }
    }

    @Override
    public Optional<String> resolve(CheckContext ctx) {
        return ctx.resolvedTarget(point)
                .flatMap(target -> Optional.ofNullable(target.properties().get(property)));
    }
}
