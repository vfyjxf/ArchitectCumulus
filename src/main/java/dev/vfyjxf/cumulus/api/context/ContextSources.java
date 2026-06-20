package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.context.sources.CapturePositionSource;
import dev.vfyjxf.cumulus.api.context.sources.CapturePropertySource;
import dev.vfyjxf.cumulus.api.context.sources.CaptureValueSource;
import dev.vfyjxf.cumulus.api.context.sources.ContextRefSource;
import dev.vfyjxf.cumulus.api.context.sources.LiteralSource;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import java.util.Objects;

public final class ContextSources {

    private ContextSources() {
    }

    public static CaptureBuilder capture(StructurePointId point) {
        return new CaptureBuilder(point);
    }

    public static CaptureBuilder capture(String pointId) {
        return new CaptureBuilder(new StructurePointId(pointId));
    }

    public static <T> ContextRefSource<T> contextRef(ContextKey<T> key) {
        return new ContextRefSource<>(key);
    }

    public static <T> LiteralSource<T> literal(T value) {
        return new LiteralSource<>(value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ContextSource<?> prefix(ContextSource<?> source, String occurrenceId) {
        if (occurrenceId == null || occurrenceId.isBlank()) {
            return source;
        }
        var prefix = occurrenceId + "/";
        if (source instanceof CapturePropertySource cps) {
            return new CapturePropertySource(
                new StructurePointId(prefix + cps.point().value()),
                cps.property());
        }
        if (source instanceof CaptureValueSource cvs) {
            return new CaptureValueSource(
                new StructurePointId(prefix + cvs.point().value()));
        }
        if (source instanceof CapturePositionSource cps) {
            return new CapturePositionSource(
                new StructurePointId(prefix + cps.point().value()));
        }
        if (source instanceof ContextRefSource<?> crs) {
            return new ContextRefSource(
                ContextKey.string(prefix + crs.key().name()));
        }
        if (source instanceof LiteralSource<?>) {
            return source;
        }
        throw new IllegalArgumentException(
            "Cannot prefix ContextSource of type " + source.getClass().getName()
                + "; implement a prefix helper or use a built-in source type");
    }

    public record CaptureBuilder(StructurePointId point) {
        public CapturePropertySource property(String name) {
            return new CapturePropertySource(point, name);
        }
        public CaptureValueSource value() {
            return new CaptureValueSource(point);
        }
        public CapturePositionSource position() {
            return new CapturePositionSource(point);
        }
    }
}
