package dev.vfyjxf.cumulus.api.target;

import net.minecraft.resources.ResourceLocation;

/**
 * Strongly-typed marker for a target category. Replaces the legacy
 * {@code ResourceLocation typeId} field on {@link TargetDescriptor}.
 *
 * <p>Open interface — mods can implement this directly to declare custom
 * target categories. Built-in implementations live in the {@code types/}
 * subpackage and cover the 7 standard categories: block, block_entity, entity,
 * fluid, capability, context, region.
 *
 * <p>Each implementation should expose its own {@code Properties} record
 * implementing {@link TargetProperties}; the record's fields replace the
 * previous {@code Map<String, String> properties} bag.
 *
 * @see TargetTypes for registry lookup and modder-facing registration
 */
public interface TargetType {

    /** Stable identifier — used as the wire-format key and registry lookup. */
    ResourceLocation id();

    /**
     * Class of the typed properties record this target type carries.
     * Used by serialization to round-trip the typed form.
     */
    Class<? extends TargetProperties> propertiesType();
}
