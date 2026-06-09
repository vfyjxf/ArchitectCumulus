package dev.vfyjxf.cumulus.api.predicate;

import java.util.Map;

public record PredicateTarget(
        String value,
        Map<String, String> properties) {
    public PredicateTarget {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Predicate target value must not be blank");
        }
        properties = Map.copyOf(properties);
    }
}
