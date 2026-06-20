package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.sources.CapturePositionSource;
import dev.vfyjxf.cumulus.api.context.sources.CapturePropertySource;
import dev.vfyjxf.cumulus.api.context.sources.CaptureValueSource;
import dev.vfyjxf.cumulus.api.context.sources.ContextRefSource;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.types.ContextKey;

/**
 * Parses legacy v1 context source expressions like {@code "controller.facing"}
 * into typed {@link ContextSource} instances. Used by the codec at load time
 * so the runtime never needs to deal with string expressions.
 */
public final class LegacySourceParser {

    private LegacySourceParser() {}

    /**
     * Parse a v1 expression string into a typed source.
     * <ul>
     *   <li>{@code "key_name"} (no dot) → {@link ContextRefSource}</li>
     *   <li>{@code "pointId.value"} → {@link CaptureValueSource}</li>
     *   <li>{@code "pointId.position"} → {@link CapturePositionSource}</li>
     *   <li>{@code "pointId.property"} → {@link CapturePropertySource}</li>
     * </ul>
     */
    public static ContextSource parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Legacy source expression must not be blank");
        }
        var dot = expression.indexOf('.');
        if (dot <= 0 || dot == expression.length() - 1) {
            // No dot — treat as a context key reference
            return new ContextRefSource(ContextKey.string(expression));
        }
        var pointId = expression.substring(0, dot);
        var property = expression.substring(dot + 1);
        var point = new StructurePointId(pointId);
        return switch (property) {
            case "value" -> new CaptureValueSource(point);
            case "position" -> new CapturePositionSource(point);
            default -> new CapturePropertySource(point, property);
        };
    }
}
