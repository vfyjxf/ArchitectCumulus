package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.sources.CaptureField;
import dev.vfyjxf.cumulus.api.context.sources.CaptureSource;
import dev.vfyjxf.cumulus.api.context.sources.ContextRefSource;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.types.ContextKey;

public final class LegacySourceParser {

    private LegacySourceParser() {}

    public static ContextSource parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Legacy source expression must not be blank");
        }
        var dot = expression.indexOf('.');
        if (dot <= 0 || dot == expression.length() - 1) {
            return new ContextRefSource<>(ContextKey.string(expression));
        }
        var pointId = expression.substring(0, dot);
        var property = expression.substring(dot + 1);
        var point = new StructurePointId(pointId);
        return switch (property) {
            case "value" -> new CaptureSource(point, CaptureField.value());
            case "position" -> new CaptureSource(point, CaptureField.position());
            default -> new CaptureSource(point, CaptureField.property(property));
        };
    }
}
