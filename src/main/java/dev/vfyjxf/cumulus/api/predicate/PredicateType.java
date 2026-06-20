package dev.vfyjxf.cumulus.api.predicate;

import net.minecraft.resources.ResourceLocation;

/**
 * Strongly-typed marker for a predicate category. Replaces the legacy
 * {@code ResourceLocation id} field on {@link PredicateDescriptor}.
 *
 * <p>Open interface — mods can implement this directly to declare custom
 * predicate categories. Built-in implementations live in the {@code types/}
 * subpackage and cover the standard categories: id, tag, state_property,
 * block_entity_type, capability_present, capability_amount, fluid_id.
 *
 * @see PredicateTypes for registry lookup
 */
public interface PredicateType {

    /** Stable identifier — used as wire-format key and registry lookup. */
    ResourceLocation id();

    /**
     * Class of the typed properties record this predicate type carries.
     * Used by serialization to round-trip the typed form.
     */
    Class<? extends PredicateProperties> propertiesType();
}
