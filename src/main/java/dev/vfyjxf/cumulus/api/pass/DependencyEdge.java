package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.engine.InstanceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 一个依赖边：from -> to。
 *
 * <p>{@link #kind()} 用于表达依赖类型（例如 "power" / "fluid"），便于调试与策略化处理。
 */
public record DependencyEdge(InstanceKey from, InstanceKey to, ResourceLocation kind) {
    public DependencyEdge {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(kind, "kind");
    }
}
