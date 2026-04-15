package dev.vfyjxf.cumulus.api.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 结构实例的键：{@code (schemaId, anchorPos)}。
 *
 * <p>v1 设计将维度/Level 绑定在 {@link ArchitectEngine} 上，因此 key 本身不包含维度信息。
 */
public record InstanceKey(ResourceLocation schemaId, BlockPos anchorPos) {
    public InstanceKey {
        Objects.requireNonNull(schemaId, "schemaId");
        Objects.requireNonNull(anchorPos, "anchorPos");
    }
}
