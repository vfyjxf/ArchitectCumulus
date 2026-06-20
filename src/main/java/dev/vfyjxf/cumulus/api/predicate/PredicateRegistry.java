package dev.vfyjxf.cumulus.api.predicate;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record PredicateRegistry(Map<ResourceLocation, PredicateMatcher> matchers) {
    private static final ResourceLocation property = ResourceLocation.parse("cumulus:property");
    private static final ResourceLocation id = ResourceLocation.parse("cumulus:id");

    public PredicateRegistry {
        matchers = Map.copyOf(matchers);
    }

    public static PredicateRegistry defaults() {
        return new PredicateRegistry(Map.of(id, PredicateRegistry::matchesId, property, PredicateRegistry::matchesProperty));
    }

    public PredicateRegistry register(ResourceLocation id, PredicateMatcher matcher) {
        if (id == null) {
            throw new IllegalArgumentException("Predicate matcher id must not be null");
        }
        if (matcher == null) {
            throw new IllegalArgumentException("Predicate matcher must not be null");
        }
        var next = new LinkedHashMap<>(matchers);
        next.put(id, matcher);
        return new PredicateRegistry(next);
    }

    public Optional<Boolean> evaluate(PredicateDescriptor predicate, PredicateTarget target) {
        if (predicate == null) {
            throw new IllegalArgumentException("Predicate descriptor must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Snapshot target must not be null");
        }
        var matcher = matchers.get(predicate.id());
        if (matcher == null && predicate.properties().isEmpty()) {
            matcher = matchers.get(id);
        }
        if (matcher == null) {
            return Optional.empty();
        }
        return Optional.of(matcher.matches(predicate, target));
    }

    private static boolean matchesId(PredicateDescriptor predicate, PredicateTarget target) {
        return predicate.properties().isEmpty() && (predicate.id().toString().equals(target.value()) || predicate.id().getPath().equals(target.value()));
    }

    private static boolean matchesProperty(PredicateDescriptor predicate, PredicateTarget target) {
        var key = predicate.properties().get("key");
        var value = predicate.properties().get("value");
        return value != null && value.equals(target.properties().get(key));
    }
}
