package dev.vfyjxf.cumulus.api.target;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class Targets {
    private Targets() {
    }

    public static TargetPattern block() {
        return new TargetPattern(
                new TargetDescriptor(ResourceLocation.parse("cumulus:block"), Map.of()),
                List.of(),
                Set.of());
    }
}
