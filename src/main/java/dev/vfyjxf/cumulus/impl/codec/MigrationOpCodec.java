package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.migration.MigrationOp;
import dev.vfyjxf.cumulus.api.migration.MigrationOps;
import dev.vfyjxf.cumulus.api.migration.MutableRuntimeContext;
import dev.vfyjxf.cumulus.api.types.ContextKey;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Codec helper for {@link MigrationOp}. Round-tripes through a discriminated
 * wire shape: {@code {"op": "...", ...fields}}.
 *
 * <p>Supported op kinds (matching {@link MigrationOps} factories):
 * <ul>
 *   <li>{@code rename} — {@link MigrationOps.RenameKeyOp}</li>
 *   <li>{@code add_default} — {@link MigrationOps.AddDefaultOp}</li>
 *   <li>{@code remove} — {@link MigrationOps.RemoveKeyOp}</li>
 *   <li>{@code transform_value} — not serializable (Java lambda); throws</li>
 *   <li>{@code custom} — serialized by id, lookup at reload time</li>
 * </ul>
 *
 * <p>For {@code transform_value} and lambda-backed {@code custom} ops the codec
 * throws — these are Java-DSL-only and can't cross the wire.
 */
public final class MigrationOpCodec {

    private MigrationOpCodec() {}

    /**
     * Helper that bypasses the generic {@code <T>} constraint on
     * {@link MigrationOps#addDefault} for cases where the value type is only
     * known at runtime (deserialization). The resulting op still type-checks
     * its values at apply time via {@link MutableRuntimeContext#putRaw}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MigrationOp addDefaultUnchecked(ContextKey<?> key, String value) {
        // Use a raw-typed op via the public factory; the put will validate at apply time.
        return new MigrationOps.AddDefaultOp((ContextKey) key, value);
    }

    public static Map<String, Object> serialize(MigrationOp op) {
        Objects.requireNonNull(op, "MigrationOp must not be null");
        var map = new LinkedHashMap<String, Object>();
        if (op instanceof MigrationOps.RenameKeyOp r) {
            map.put("op", "rename");
            map.put("from", ContextKeyCodec.serialize(r.from()));
            map.put("to", ContextKeyCodec.serialize(r.to()));
        } else if (op instanceof MigrationOps.AddDefaultOp<?> a) {
            map.put("op", "add_default");
            map.put("key", ContextKeyCodec.serialize(a.key()));
            map.put("value", String.valueOf(a.value()));
        } else if (op instanceof MigrationOps.RemoveKeyOp rm) {
            map.put("op", "remove");
            map.put("key", ContextKeyCodec.serialize(rm.key()));
        } else if (op instanceof MigrationOps.CustomOp c) {
            map.put("op", "custom");
            map.put("id", c.id().toString());
            // Don't serialize the lambda — caller must re-register on load.
        } else {
            throw new IllegalArgumentException(
                            "Cannot serialize MigrationOp of type " + op.getClass().getName() + " (transform_value and lambda custom ops are Java-DSL-only)");
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static MigrationOp deserialize(Map<String, Object> wire) {
        Objects.requireNonNull(wire, "Wire map must not be null");
        var op = String.valueOf(wire.get("op"));
        return switch (op) {
            case "rename" -> {
                var from = ContextKeyCodec.deserialize((Map<String, String>) wire.get("from"));
                var to = ContextKeyCodec.deserialize((Map<String, String>) wire.get("to"));
                yield MigrationOps.rename(from, to);
            }
            case "add_default" -> {
                var keyWire = (Map<String, String>) wire.get("key");
                var key = ContextKeyCodec.deserialize(keyWire);
                var value = String.valueOf(wire.get("value"));
                yield addDefaultUnchecked(key, value);
            }
            case "remove" -> {
                var keyWire = (Map<String, String>) wire.get("key");
                yield MigrationOps.remove(ContextKeyCodec.deserialize(keyWire));
            }
            case "custom" -> {
                var id = ResourceLocation.parse(String.valueOf(wire.get("id")));
                // No-op deserialization: caller must register a real handler under this id
                // before the migration runs. Until then this is a placeholder.
                yield MigrationOps.custom(id, (Consumer<MutableRuntimeContext>) ctx -> {
                    throw new IllegalStateException(
                                    "Custom migration op " + id + " has no registered handler");
                });
            }
            default -> throw new IllegalArgumentException("Unknown MigrationOp kind: " + op);
        };
    }
}
