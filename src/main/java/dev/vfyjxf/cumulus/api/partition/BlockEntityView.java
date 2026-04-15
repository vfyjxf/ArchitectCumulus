package dev.vfyjxf.cumulus.api.partition;

import net.minecraft.resources.ResourceLocation;

/**
 * 方块实体只读视图。
 *
 * <p>该视图既可包装 MC 的 {@code BlockEntity}，也可由缓存/快照实现。
 */
public interface BlockEntityView {
    /**
     * 方块实体类型 id（如 {@code minecraft:chest}）。
     */
    ResourceLocation typeId();

    /**
     * 原始对象（实现自定义；可用于可视化/调试/与宿主系统交互）。
     */
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
