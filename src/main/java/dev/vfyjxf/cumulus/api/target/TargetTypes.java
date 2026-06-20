package dev.vfyjxf.cumulus.api.target;

import dev.vfyjxf.cumulus.api.target.types.BlockEntityTargetType;
import dev.vfyjxf.cumulus.api.target.types.BlockTargetType;
import dev.vfyjxf.cumulus.api.target.types.CapabilityTargetType;
import dev.vfyjxf.cumulus.api.target.types.ContextTargetType;
import dev.vfyjxf.cumulus.api.target.types.EntityTargetType;
import dev.vfyjxf.cumulus.api.target.types.FluidTargetType;
import dev.vfyjxf.cumulus.api.target.types.RegionTargetType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry of all known {@link TargetType}s. Built-in types are pre-registered;
 * mods add their own during common setup:
 *
 * <pre>{@code
 * FMLCommonSetupEvent.enqueueWork(() ->
 *     TargetTypes.register(MyModTargetTypes.CUSTOM));
 * }</pre>
 *
 * <p>Lookups ({@link #fromId(ResourceLocation)}) never throw — unknown ids
 * return empty Optional so callers can produce a clear RUNTIME_ONLY_UNAVAILABLE
 * diagnostic instead of crashing.
 */
public final class TargetTypes {

    private static final Map<ResourceLocation, TargetType> entries = new LinkedHashMap<>();

    static {
        for (var type : List.of(
                BlockTargetType.instance,
                BlockEntityTargetType.instance,
                EntityTargetType.instance,
                FluidTargetType.instance,
                CapabilityTargetType.instance,
                ContextTargetType.instance,
                RegionTargetType.instance)) {
            entries.put(type.id(), type);
        }
    }

    private TargetTypes() {
    }

    /** Register a custom target type. Re-registering replaces the entry. */
    public static synchronized void register(TargetType type) {
        Objects.requireNonNull(type, "TargetType must not be null");
        entries.put(type.id(), type);
    }

    /** Look up by id; empty if not registered. */
    public static Optional<TargetType> maybeFromId(ResourceLocation id) {
        Objects.requireNonNull(id, "TargetType id must not be null");
        return Optional.ofNullable(entries.get(id));
    }

    /**
     * Look up by id; throws {@link IllegalArgumentException} if not registered.
     * Prefer {@link #maybeFromId} in code paths that handle unknown types
     * gracefully.
     */
    public static TargetType fromId(ResourceLocation id) {
        return maybeFromId(id).orElseThrow(() -> new IllegalArgumentException(
                "Unknown TargetType: " + id));
    }

    /** True if a type is registered under this id. */
    public static boolean isRegistered(ResourceLocation id) {
        return entries.containsKey(id);
    }

    /** Number of registered types (built-in + custom). */
    public static int size() {
        return entries.size();
    }
}
