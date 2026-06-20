package dev.vfyjxf.cumulus.api.predicate;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PredicateDescriptor(ResourceLocation id, Map<String, String> properties) {
    public PredicateDescriptor {
        if (id == null) {
            throw new IllegalArgumentException("Predicate id must not be null");
        }
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }
}
