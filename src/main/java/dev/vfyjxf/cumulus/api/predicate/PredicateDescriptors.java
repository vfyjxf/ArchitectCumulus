package dev.vfyjxf.cumulus.api.predicate;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

public final class PredicateDescriptors {
    private PredicateDescriptors() {
    }

    public static PredicateDescriptor id(String id) {
        return new PredicateDescriptor(ResourceLocation.parse(id), Map.of());
    }

    public static PredicateDescriptor property(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Predicate property key must not be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Predicate property value must not be null");
        }
        return new PredicateDescriptor(ResourceLocation.parse("cumulus:property"), Map.of(
                "key", key,
                "value", value));
    }

    public static PredicateDescriptor fromSerialized(Map<String, String> values) {
        if (values == null) {
            throw new IllegalArgumentException("Serialized predicate values must not be null");
        }
        var id = values.get("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Serialized predicate id must not be blank");
        }
        var properties = new TreeMap<String, String>();
        for (var entry : values.entrySet()) {
            if (!entry.getKey().equals("id")) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        return new PredicateDescriptor(ResourceLocation.parse(id), properties);
    }
}
