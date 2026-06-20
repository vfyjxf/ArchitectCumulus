package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.sources.CaptureField;
import dev.vfyjxf.cumulus.api.context.sources.CaptureSource;
import dev.vfyjxf.cumulus.api.context.sources.ContextRefSource;
import dev.vfyjxf.cumulus.api.context.sources.LiteralSource;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ContextSourceCodec {

    private ContextSourceCodec() {}

    public static Map<String, Object> serialize(ContextSource source) {
        Objects.requireNonNull(source, "ContextSource must not be null");
        var map = new LinkedHashMap<String, Object>();
        if (source instanceof CaptureSource cs) {
            map.put("kind", "capture");
            map.put("point", cs.point().value());
            switch (cs.field()) {
                case CaptureField.Value v -> map.put("field", "value");
                case CaptureField.Position p -> map.put("field", "position");
                case CaptureField.Property prop -> {
                    map.put("field", "property");
                    map.put("property", prop.name());
                }
            }
        } else if (source instanceof ContextRefSource<?> crs) {
            map.put("kind", "context_ref");
            map.put("key", ContextKeyCodec.serialize(crs.key()));
        } else if (source instanceof LiteralSource<?> ls) {
            map.put("kind", "literal");
            map.put("value", String.valueOf(ls.value()));
        } else {
            map.put("kind", "unknown");
            map.put("class", source.getClass().getName());
        }
        return map;
    }

    public static ContextSource deserialize(Map<String, Object> wire) {
        Objects.requireNonNull(wire, "Wire map must not be null");
        var kind = String.valueOf(wire.get("kind"));
        // Backward compat: old wire kinds map to unified "capture"
        if (kind.equals("capture_value") || kind.equals("capture_position") || kind.equals("capture_property")) {
            kind = "capture";
        }

        return switch (kind) {
            case "capture" -> {
                var point = new StructurePointId(String.valueOf(wire.get("point")));
                var field = String.valueOf(wire.getOrDefault("field", "value"));
                yield switch (field) {
                    case "position" -> new CaptureSource(point, CaptureField.position());
                    case "property" -> new CaptureSource(point, CaptureField.property(String.valueOf(wire.get("property"))));
                    default -> new CaptureSource(point, CaptureField.value());
                };
            }
            case "context_ref" -> {
                @SuppressWarnings("unchecked")
                var keyWire = (Map<String, String>) wire.get("key");
                yield new ContextRefSource<>(ContextKeyCodec.deserialize(keyWire));
            }
            case "literal" -> new LiteralSource<>(String.valueOf(wire.get("value")));
            default -> throw new IllegalArgumentException("Unknown ContextSource kind: " + kind);
        };
    }
}
