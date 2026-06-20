package dev.vfyjxf.cumulus.api.predicate;

import dev.vfyjxf.cumulus.api.predicate.types.IdPredicateType;
import dev.vfyjxf.cumulus.api.predicate.types.StatePropertyPredicateType;
import dev.vfyjxf.cumulus.api.predicate.types.TagPredicateType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry of all known {@link PredicateType}s. Built-in types are pre-registered;
 * mods add their own during common setup.
 */
public final class PredicateTypes {

    private static final Map<ResourceLocation, PredicateType> entries = new LinkedHashMap<>();

    static {
        for (var type : List.of(
                IdPredicateType.instance,
                TagPredicateType.instance,
                StatePropertyPredicateType.instance)) {
            entries.put(type.id(), type);
        }
    }

    private PredicateTypes() {
    }

    public static synchronized void register(PredicateType type) {
        Objects.requireNonNull(type, "PredicateType must not be null");
        entries.put(type.id(), type);
    }

    public static Optional<PredicateType> maybeFromId(ResourceLocation id) {
        Objects.requireNonNull(id, "PredicateType id must not be null");
        return Optional.ofNullable(entries.get(id));
    }

    public static PredicateType fromId(ResourceLocation id) {
        return maybeFromId(id).orElseThrow(() -> new IllegalArgumentException(
                "Unknown PredicateType: " + id));
    }

    public static boolean isRegistered(ResourceLocation id) {
        return entries.containsKey(id);
    }

    public static int size() {
        return entries.size();
    }
}
