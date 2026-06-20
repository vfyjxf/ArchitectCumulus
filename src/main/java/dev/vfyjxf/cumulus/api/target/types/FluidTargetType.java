package dev.vfyjxf.cumulus.api.target.types;

import dev.vfyjxf.cumulus.api.target.TargetProperties;
import dev.vfyjxf.cumulus.api.target.TargetType;
import net.minecraft.resources.ResourceLocation;

/**
 * Target category: a fluid block (lava, water, mod fluids).
 * No extra properties — the predicate chain discriminates by fluid id.
 */
public final class FluidTargetType implements TargetType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:fluid");
    public static final FluidTargetType instance = new FluidTargetType();

    private FluidTargetType() {
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    /** Fluid target carries no extra properties. */
    public record Props() implements TargetProperties {
        public static final Props instance = new Props();
    }
}
