package dev.vfyjxf.cumulus.api.predicate.types;

import dev.vfyjxf.cumulus.api.predicate.PredicateProperties;
import dev.vfyjxf.cumulus.api.predicate.PredicateType;

import java.util.Objects;

/** Predicate: a BlockState property equals an expected value. */
public final class StatePropertyPredicateType implements PredicateType {

    public static final net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse("cumulus:property");
    public static final StatePropertyPredicateType instance = new StatePropertyPredicateType();

    private StatePropertyPredicateType() {}

    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends PredicateProperties> propertiesType() {
        return Props.class;
    }

    /** Properties: property name + expected value. */
    public record Props(String property, String expected) implements PredicateProperties {
        public Props {
            if (property == null || property.isBlank()) {
                throw new IllegalArgumentException("property name must not be blank");
            }
            Objects.requireNonNull(expected, "expected value must not be null");
        }
    }
}
