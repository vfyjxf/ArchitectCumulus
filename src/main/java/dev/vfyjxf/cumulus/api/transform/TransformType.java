package dev.vfyjxf.cumulus.api.transform;

import net.minecraft.resources.ResourceLocation;

/**
 * Strongly-typed marker for a transform category. Replaces the legacy
 * {@code ResourceLocation id} field on {@link TransformDescriptor}.
 *
 * <p>Open interface — mods can implement directly. Built-in implementations
 * live in the {@code types/} subpackage: identity, horizontal_facing,
 * horizontal_mirror, rotate_y_90/180/270.
 */
public interface TransformType {

    /** Stable identifier — wire-format key and registry lookup. */
    ResourceLocation id();

    /** Typed properties record class. */
    Class<? extends TransformProperties> propertiesType();
}
