package dev.vfyjxf.cumulus.api.transform.types;

import dev.vfyjxf.cumulus.api.transform.TransformProperties;
import dev.vfyjxf.cumulus.api.transform.TransformType;

import net.minecraft.resources.ResourceLocation;

/** Transform: no rotation. The identity transform. Carries no properties. */
public final class IdentityTransformType implements TransformType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:identity");
    public static final IdentityTransformType instance = new IdentityTransformType();

    private IdentityTransformType() {}

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TransformProperties> propertiesType() {
        return Props.class;
    }

    public record Props() implements TransformProperties {
        public static final Props instance = new Props();
    }
}
