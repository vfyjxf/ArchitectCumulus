package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public interface TargetSnapshotSource {
    Optional<SnapshotTarget> target(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max);

    default Optional<SnapshotTarget> target(
            ResourceLocation targetType,
            RelativeBlockPos min,
            RelativeBlockPos max,
            Map<String, String> targetProperties) {
        return target(targetType, min, max);
    }

    Optional<String> context(String key);
}
