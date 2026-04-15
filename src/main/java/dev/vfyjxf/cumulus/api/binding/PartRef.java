package dev.vfyjxf.cumulus.api.binding;

import net.minecraft.resources.ResourceLocation;

/**
 * 一个“部件引用”。
 *
 * <p>部件引用只表达“结构由哪些东西组成”，不要求直接包含世界对象本身。
 * 典型实现：
 * <ul>
 *     <li>{@link BlockPartRef}：方块坐标</li>
 *     <li>{@link BlockEntityPartRef}：方块实体坐标</li>
 *     <li>{@link EntityPartRef}：实体 UUID</li>
 * </ul>
 *
 * <p>{@link #kind()} 用于做多态分发与数据化；同一个 role 下可以混合不同 kind（取决于 binder 设计）。
 */
public interface PartRef {
    /**
     * 部件类型（稳定 id）。
     */
    ResourceLocation kind();
}
