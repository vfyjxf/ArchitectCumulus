package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.context.sources.CaptureSource;
import dev.vfyjxf.cumulus.api.context.sources.CaptureField;
import dev.vfyjxf.cumulus.api.context.sources.ContextRefSource;
import dev.vfyjxf.cumulus.api.context.sources.LiteralSource;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.types.ContextKey;

public final class ContextSources {

    private ContextSources() {}

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
        if (source instanceof CaptureSource cs) {
            return new CaptureSource(
                            new StructurePointId(prefix + cs.point().value()),
                            cs.field());
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
        public CaptureSource property(String name) {
            return new CaptureSource(point, CaptureField.property(name));
        }

        public CaptureSource value() {
            return new CaptureSource(point, CaptureField.value());
        }

        public CaptureSource position() {
            return new CaptureSource(point, CaptureField.position());
        }
    }
}
