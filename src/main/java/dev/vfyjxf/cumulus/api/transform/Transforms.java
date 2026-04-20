package dev.vfyjxf.cumulus.api.transform;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class Transforms {
    private Transforms() {
    }

    public static TransformDescriptor identity() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:identity"), Map.of());
    }

    public static TransformDescriptor horizontalFacing() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:horizontal_facing"), Map.of());
    }
}
