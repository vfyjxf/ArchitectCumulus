package dev.vfyjxf.cumulus.api.target.types;

import dev.vfyjxf.cumulus.api.target.TargetProperties;
import dev.vfyjxf.cumulus.api.target.TargetType;
import net.minecraft.resources.ResourceLocation;

/**
 * Target category: a single Minecraft block (with state + optional BE NBT).
 * Carries no extra properties — everything beyond "it's a block" lives in the
 * predicate chain. This is the most common target type for casing/controller
 * points.
 */
public final class BlockTargetType implements TargetType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:block");
    public static final BlockTargetType instance = new BlockTargetType();
    public static final Props props = new Props();

    private BlockTargetType() {
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    /** Block target carries no extra properties. */
    public record Props() implements TargetProperties {
        public static final Props instance = new Props();
    }
}
