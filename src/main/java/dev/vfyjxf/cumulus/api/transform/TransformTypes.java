package dev.vfyjxf.cumulus.api.transform;

import dev.vfyjxf.cumulus.api.transform.types.HorizontalFacingTransformType;
import dev.vfyjxf.cumulus.api.transform.types.IdentityTransformType;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Registry of all known {@link TransformType}s. */
public final class TransformTypes {

    private static final Map<ResourceLocation, TransformType> entries = new LinkedHashMap<>();

    static {
        for (var type : List.of(IdentityTransformType.instance, HorizontalFacingTransformType.instance)) {
            entries.put(type.id(), type);
        }
    }

    private TransformTypes() {}

    public static synchronized void register(TransformType type) {
        Objects.requireNonNull(type, "TransformType must not be null");
        entries.put(type.id(), type);
    }

    public static Optional<TransformType> maybeFromId(ResourceLocation id) {
        Objects.requireNonNull(id, "TransformType id must not be null");
        return Optional.ofNullable(entries.get(id));
    }

    public static TransformType fromId(ResourceLocation id) {
        return maybeFromId(id).orElseThrow(() -> new IllegalArgumentException(
                        "Unknown TransformType: " + id));
    }

    public static boolean isRegistered(ResourceLocation id) {
        return entries.containsKey(id);
    }

    public static int size() {
        return entries.size();
    }
}
