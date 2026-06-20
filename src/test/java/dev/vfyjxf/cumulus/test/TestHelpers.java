package dev.vfyjxf.cumulus.test;

import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Test-only helpers for building legacy-shape descriptors.
 */
public final class TestHelpers {

    private TestHelpers() {}

    /**
     * Build a {@link PredicateDescriptor} that mirrors the legacy v1
     * {@code new PredicateDescriptor(ResourceLocation.parse(id), props)} shape.
     *
     * <p>If {@code props} is empty, the descriptor wraps the id with empty
     * properties — matching the legacy "predicate id is itself the match
     * value, no extra properties" convention that {@code PredicateRegistry}
     * falls back to via its ID matcher.
     */
    public static PredicateDescriptor legacyPredicate(String id, Map<String, String> props) {
        var idRl = ResourceLocation.parse(id);
        if (props == null || props.isEmpty()) {
            return new PredicateDescriptor(idRl, Map.of());
        }
        return new PredicateDescriptor(idRl, props);
    }

    /** Build a predicate from a single id value (most common legacy case). */
    public static PredicateDescriptor legacyIdPredicate(String id) {
        return new PredicateDescriptor(
                        ResourceLocation.parse("cumulus:id"),
                        Map.of("id", id));
    }
}
