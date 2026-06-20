package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.context.ContextSource;
import dev.vfyjxf.cumulus.api.context.sources.CapturePositionSource;
import dev.vfyjxf.cumulus.api.context.sources.CapturePropertySource;
import dev.vfyjxf.cumulus.api.context.sources.CaptureValueSource;
import dev.vfyjxf.cumulus.api.context.sources.ContextRefSource;
import dev.vfyjxf.cumulus.api.context.sources.LiteralSource;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Codec helper for {@link ContextSource}. Round-trips the typed source through
 * a discriminated wire shape: {@code {"kind": "...", ...fields}}.
 *
 * <p>Supported kinds:
 * <ul>
 *   <li>{@code capture_property} — {@link CapturePropertySource}</li>
 *   <li>{@code capture_value} — {@link CaptureValueSource}</li>
 *   <li>{@code capture_position} — {@link CapturePositionSource}</li>
 *   <li>{@code context_ref} — {@link ContextRefSource}</li>
 *   <li>{@code literal} — {@link LiteralSource}</li>
 * </ul>
 */
public final class ContextSourceCodec {

    private ContextSourceCodec() {}

    public static Map<String, Object> serialize(ContextSource source) {
        Objects.requireNonNull(source, "ContextSource must not be null");
        var map = new LinkedHashMap<String, Object>();
        if (source instanceof CapturePropertySource cps) {
            map.put("kind", "capture_property");
            map.put("point", cps.point().value());
            map.put("property", cps.property());
        } else if (source instanceof CaptureValueSource cvs) {
            map.put("kind", "capture_value");
            map.put("point", cvs.point().value());
        } else if (source instanceof CapturePositionSource cps) {
            map.put("kind", "capture_position");
            map.put("point", cps.point().value());
        } else if (source instanceof ContextRefSource crs) {
            map.put("kind", "context_ref");
            map.put("key", ContextKeyCodec.serialize(crs.key()));
        } else if (source instanceof LiteralSource ls) {
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
        return switch (kind) {
            case "capture_property" -> new CapturePropertySource(
                            new StructurePointId(String.valueOf(wire.get("point"))),
                            String.valueOf(wire.get("property")));
            case "capture_value" -> new CaptureValueSource(
                            new StructurePointId(String.valueOf(wire.get("point"))));
            case "capture_position" -> new CapturePositionSource(
                            new StructurePointId(String.valueOf(wire.get("point"))));
            case "context_ref" -> {
                @SuppressWarnings("unchecked")
                var keyWire = (Map<String, String>) wire.get("key");
                yield new ContextRefSource(ContextKeyCodec.deserialize(keyWire));
            }
            case "literal" -> new LiteralSource(String.valueOf(wire.get("value")));
            default -> throw new IllegalArgumentException("Unknown ContextSource kind: " + kind);
        };
    }
}
