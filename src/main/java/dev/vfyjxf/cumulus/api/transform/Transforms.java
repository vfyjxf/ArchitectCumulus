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

    public static TransformDescriptor horizontalMirror() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:horizontal_mirror"), Map.of());
    }

    public static TransformDescriptor rotateY90() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:rotate_y_90"), Map.of());
    }

    public static TransformDescriptor rotateY180() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:rotate_y_180"), Map.of());
    }

    public static TransformDescriptor rotateY270() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:rotate_y_270"), Map.of());
    }

    public static TransformDescriptor rotations3d() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:rotations_3d"), Map.of());
    }

    public static TransformDescriptor custom(ResourceLocation id, Map<String, String> properties) {
        return new TransformDescriptor(id, properties);
    }
}
