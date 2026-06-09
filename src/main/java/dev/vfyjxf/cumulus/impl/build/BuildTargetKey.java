package dev.vfyjxf.cumulus.impl.build;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import net.minecraft.resources.ResourceLocation;

public record BuildTargetKey(
        ResourceLocation targetType,
        RelativeBlockPos min,
        RelativeBlockPos max) {
    public BuildTargetKey {
        if (targetType == null) {
            throw new IllegalArgumentException("Build target key type must not be null");
        }
        if (min == null) {
            throw new IllegalArgumentException("Build target key min must not be null");
        }
        if (max == null) {
            throw new IllegalArgumentException("Build target key max must not be null");
        }
    }

    public static BuildTargetKey from(BuildAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Build action must not be null");
        }
        return new BuildTargetKey(action.targetType(), action.min(), action.max());
    }
}
