package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.engine.InstanceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record DependencyEdge(InstanceKey from, InstanceKey to, ResourceLocation kind) {
    public DependencyEdge {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(kind, "kind");
    }
}
