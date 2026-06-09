package dev.vfyjxf.cumulus.api.target;

import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record TargetPattern(
        TargetDescriptor target,
        List<PredicateDescriptor> predicates,
        Set<String> captures) {
    public TargetPattern {
        predicates = List.copyOf(predicates);
        captures = Set.copyOf(captures);
    }

    public TargetPattern where(PredicateDescriptor predicate) {
        var next = new ArrayList<>(predicates);
        next.add(predicate);
        return new TargetPattern(target, next, captures);
    }

    public TargetPattern capture(String captureName) {
        var next = new LinkedHashSet<>(captures);
        next.add(captureName);
        return new TargetPattern(target, predicates, next);
    }

    public TargetPattern anchor() {
        return property("anchor", "true");
    }

    public TargetPattern property(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Target property key must not be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Target property value must not be null");
        }
        var properties = new LinkedHashMap<>(target.properties());
        properties.put(key, value);
        return new TargetPattern(
                new TargetDescriptor(target.typeId(), Map.copyOf(properties)),
                predicates,
                captures);
    }
}
