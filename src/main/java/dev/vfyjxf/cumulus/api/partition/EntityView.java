package dev.vfyjxf.cumulus.api.partition;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 实体只读视图。
 *
 * <p>结构检查中“实体部件”通常需要：
 * <ul>
 *     <li>uuid：稳定身份</li>
 *     <li>typeId：实体类型</li>
 *     <li>position：用于空间匹配（此处用 packed blockPos 表示）</li>
 * </ul>
 */
public interface EntityView {
    /** 实体 UUID。 */
    UUID uuid();

    /** 实体类型 id（如 {@code minecraft:zombie}）。 */
    ResourceLocation typeId();

    /**
     * 实体所在位置（按 blockPos 的 packed long 表示）。
     */
    long packedBlockPos();

    /** 原始对象（实现自定义）。 */
    Object raw();

    /**
     * tag 判断（可选）。
     *
     * <p>默认返回 false；实现如需支持标签匹配，应覆写该方法。
     */
    default boolean isInTag(ResourceLocation tagId) {
        return false;
    }
}
