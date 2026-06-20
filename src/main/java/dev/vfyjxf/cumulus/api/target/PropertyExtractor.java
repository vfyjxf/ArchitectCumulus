package dev.vfyjxf.cumulus.api.target;

import dev.vfyjxf.cumulus.api.target.types.BlockEntityTargetType;
import dev.vfyjxf.cumulus.api.target.types.CapabilityTargetType;
import dev.vfyjxf.cumulus.api.target.types.ContextTargetType;
import dev.vfyjxf.cumulus.api.target.types.EntityTargetType;
import dev.vfyjxf.cumulus.api.target.types.RegionTargetType;
import dev.vfyjxf.cumulus.api.target.types.UnknownTargetType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Internal helper: extract a {@code Map<String, String>} view from a typed
 * {@link TargetProperties} record. Used by {@link TargetDescriptor#properties()}
 * back-compat shim and the legacy serialization codec.
 *
 * <p>Skips fields holding default placeholder values (e.g. {@code cumulus:any}
 * ids, empty NBT), so {@code Targets.blockEntity()} (no-arg) round-trips to an
 * empty property map, matching the legacy v1 behavior.
 *
 * <p>Only handles built-in types. Custom target types implementing
 * {@link TargetProperties} directly should also implement
 * {@link CustomTargetProperties#asMap()} if they need the back-compat view.
 */
final class PropertyExtractor {

    private static final String anyId = "cumulus:any";
    private static final String emptyNbt = "{}";

    private PropertyExtractor() {}

    static Map<String, String> extract(TargetProperties props) {
        if (props instanceof CustomTargetProperties ctp) {
            return ctp.asMap();
        }
        var map = new LinkedHashMap<String, String>();
        if (props instanceof BlockEntityTargetType.Props p) {
            putIfNotDefault(map, "block", p.blockId().toString(), anyId);
            putIfNotDefault(map, "block_entity", p.blockEntityId().toString(), anyId);
            putIfNotDefault(map, "nbt", p.nbt(), emptyNbt);
        } else if (props instanceof EntityTargetType.Props p) {
            putIfNotDefault(map, "entity", p.entityId().toString(), anyId);
            putIfNotDefault(map, "nbt", p.nbt(), emptyNbt);
        } else if (props instanceof CapabilityTargetType.Props p) {
            map.put("name", p.capabilityName());
        } else if (props instanceof ContextTargetType.Props p) {
            map.put("key", p.key().name());
        } else if (props instanceof RegionTargetType.Props p) {
            map.put("from", p.from().asString());
            map.put("to", p.to().asString());
        } else if (props instanceof UnknownTargetType.Props p) {
            map.putAll(p.raw());
        }
        // BlockTargetType.Props, FluidTargetType.Props have no fields
        return Map.copyOf(map);
    }

    private static void putIfNotDefault(Map<String, String> map, String key, String value, String defaultValue) {
        if (!defaultValue.equals(value)) {
            map.put(key, value);
        }
    }
}
