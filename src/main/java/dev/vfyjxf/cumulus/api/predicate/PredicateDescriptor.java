package dev.vfyjxf.cumulus.api.predicate;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record PredicateDescriptor(ResourceLocation id, Map<String, String> properties) {
    public PredicateDescriptor {
        if (id == null) {
            throw new IllegalArgumentException("Predicate id must not be null");
        }
        properties = Map.copyOf(properties);
    }
}
