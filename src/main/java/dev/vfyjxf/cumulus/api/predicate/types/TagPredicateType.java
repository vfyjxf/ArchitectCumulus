package dev.vfyjxf.cumulus.api.predicate.types;

import dev.vfyjxf.cumulus.api.predicate.PredicateProperties;
import dev.vfyjxf.cumulus.api.predicate.PredicateType;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Predicate: target's id is in a tag. */
public final class TagPredicateType implements PredicateType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:tag");
    public static final TagPredicateType instance = new TagPredicateType();

    private TagPredicateType() {}

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends PredicateProperties> propertiesType() {
        return Props.class;
    }

    /** Properties: the expected tag id. */
    public record Props(ResourceLocation tagId) implements PredicateProperties {
        public Props {
            Objects.requireNonNull(tagId, "tagId must not be null");
        }
    }
}
