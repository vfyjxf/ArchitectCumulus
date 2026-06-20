package dev.vfyjxf.cumulus.api.predicate;

import dev.vfyjxf.cumulus.api.predicate.types.IdPredicateType;
import dev.vfyjxf.cumulus.api.predicate.types.StatePropertyPredicateType;
import dev.vfyjxf.cumulus.api.predicate.types.TagPredicateType;
import dev.vfyjxf.cumulus.api.predicate.types.UnknownPredicateType;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Internal helper: extract a {@code Map<String, String>} view from a typed
 * {@link PredicateProperties} record. Mirrors {@link dev.vfyjxf.cumulus.api.target.PropertyExtractor}.
 */
public final class PredicatePropertyExtractor {

    private PredicatePropertyExtractor() {}

    public static Map<String, String> extract(PredicateProperties props) {
        var map = new LinkedHashMap<String, String>();
        if (props instanceof IdPredicateType.Props p) {
            map.put("id", p.expectedId().toString());
        } else if (props instanceof TagPredicateType.Props p) {
            map.put("tag", p.tagId().toString());
        } else if (props instanceof StatePropertyPredicateType.Props p) {
            // Legacy v1 wire keys: "key" + "value"
            map.put("key", p.property());
            map.put("value", p.expected());
        } else if (props instanceof UnknownPredicateType.Props p) {
            // Preserve a stable, sorted iteration order to match the legacy
            // TreeMap-based wire format (deterministic datagen output).
            new TreeMap<>(p.raw()).forEach(map::put);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    /** Reconstruct typed Props from a legacy v1 id + property map. */
    public static PredicateProperties buildProps(PredicateType type, Map<String, String> properties) {
        if (type instanceof IdPredicateType) {
            return new IdPredicateType.Props(
                            ResourceLocation.parse(properties.getOrDefault("id", "cumulus:any")));
        }
        if (type instanceof TagPredicateType) {
            return new TagPredicateType.Props(
                            ResourceLocation.parse(properties.getOrDefault("tag", "cumulus:any")));
        }
        if (type instanceof StatePropertyPredicateType) {
            return new StatePropertyPredicateType.Props(
                            properties.getOrDefault("key", properties.getOrDefault("property", "facing")),
                            properties.getOrDefault("value", "north"));
        }
        if (type instanceof UnknownPredicateType) {
            return new UnknownPredicateType.Props(properties);
        }
        // Unknown typed predicate — fall back to a generic id-anything Props
        return new IdPredicateType.Props(ResourceLocation.parse("cumulus:any"));
    }
}
