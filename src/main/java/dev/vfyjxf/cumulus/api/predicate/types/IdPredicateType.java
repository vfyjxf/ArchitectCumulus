package dev.vfyjxf.cumulus.api.predicate.types;

import dev.vfyjxf.cumulus.api.predicate.PredicateProperties;
import dev.vfyjxf.cumulus.api.predicate.PredicateType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Predicate: target's id matches exactly (block id, entity id, fluid id, etc.). */
public final class IdPredicateType implements PredicateType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:id");
    public static final IdPredicateType instance = new IdPredicateType();

    private IdPredicateType() {
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends PredicateProperties> propertiesType() {
        return Props.class;
    }

    /** Properties: the expected id. */
    public record Props(ResourceLocation expectedId) implements PredicateProperties {
        public Props {
            Objects.requireNonNull(expectedId, "expectedId must not be null");
        }
    }
}
