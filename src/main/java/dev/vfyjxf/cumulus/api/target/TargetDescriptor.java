package dev.vfyjxf.cumulus.api.target;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record TargetDescriptor(ResourceLocation typeId, Map<String, String> properties) {
    public TargetDescriptor {
        if (typeId == null) {
            throw new IllegalArgumentException("Target type id must not be null");
        }
        properties = Map.copyOf(properties);
    }

    /**
     * Typed lookup: resolve this descriptor's {@code typeId} to a
     * {@link TargetType} via {@link TargetTypes#maybeFromId}. Returns empty
     * for unregistered ids (e.g. custom types not yet registered).
     */
    public java.util.Optional<TargetType> type() {
        return TargetTypes.maybeFromId(typeId);
    }

    /** True if this descriptor's type id is registered as the given {@link TargetType}. */
    public boolean isType(TargetType type) {
        return type != null && typeId.equals(type.id());
    }
}
