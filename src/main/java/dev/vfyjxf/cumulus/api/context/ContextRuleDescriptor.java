package dev.vfyjxf.cumulus.api.context;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record ContextRuleDescriptor(ResourceLocation id, Map<String, String> properties) {
    public ContextRuleDescriptor {
        if (id == null) {
            throw new IllegalArgumentException("Context rule id must not be null");
        }
        properties = Map.copyOf(properties);
    }

    public static ContextRuleDescriptor derive(String key, String source) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Context key must not be blank");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Context source must not be blank");
        }
        return new ContextRuleDescriptor(
                ResourceLocation.parse("cumulus:derive"),
                Map.of(
                        "key", key,
                        "source", source));
    }
}
