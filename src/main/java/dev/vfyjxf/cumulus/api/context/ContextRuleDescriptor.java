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
}
