package dev.vfyjxf.cumulus.api.transform.types;

import dev.vfyjxf.cumulus.api.transform.TransformProperties;
import dev.vfyjxf.cumulus.api.transform.TransformType;
import net.minecraft.resources.ResourceLocation;

/** Transform: 4 horizontal facing directions (N/E/S/W). Carries no properties. */
public final class HorizontalFacingTransformType implements TransformType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:horizontal_facing");
    public static final HorizontalFacingTransformType instance = new HorizontalFacingTransformType();

    private HorizontalFacingTransformType() {
    }

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
