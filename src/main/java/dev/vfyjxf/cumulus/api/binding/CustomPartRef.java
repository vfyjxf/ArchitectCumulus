package dev.vfyjxf.cumulus.api.binding;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 自定义部件引用。
 *
 * <p>用于在“方块/方块实体/实体”之外扩展新的部件类型。
 */
public record CustomPartRef(ResourceLocation kind, Object handle) implements PartRef {
    public CustomPartRef {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(handle, "handle");
    }
}
