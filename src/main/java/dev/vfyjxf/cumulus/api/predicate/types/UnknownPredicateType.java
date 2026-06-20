package dev.vfyjxf.cumulus.api.predicate.types;

import dev.vfyjxf.cumulus.api.predicate.PredicateProperties;
import dev.vfyjxf.cumulus.api.predicate.PredicateType;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;

/**
 * Fallback predicate type used when the codec encounters an id not registered
 * in {@link dev.vfyjxf.cumulus.api.predicate.PredicateTypes}. Preserves the
 * original id and raw property map so consumers can report it in diagnostics
 * and a later mod load can resolve it.
 */
public final class UnknownPredicateType implements PredicateType {

    private final ResourceLocation id;

    private UnknownPredicateType(ResourceLocation id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public static UnknownPredicateType of(ResourceLocation id) {
        return new UnknownPredicateType(id);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends PredicateProperties> propertiesType() {
        return Props.class;
    }

    /** Raw property map preserved verbatim until a real PredicateType is registered. */
    public record Props(Map<String, String> raw) implements PredicateProperties {
        public Props {
            Objects.requireNonNull(raw, "raw must not be null");
            raw = Map.copyOf(raw);
        }
    }
}
