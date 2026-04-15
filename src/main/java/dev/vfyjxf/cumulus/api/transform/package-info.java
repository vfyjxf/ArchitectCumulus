/**
 * 结构坐标变换抽象。
 *
 * <p>用于把“局部结构坐标（local）”映射到“世界坐标（world）”，以支持旋转/镜像/平移等抽象变换。
 * v1 提供最小的平移实现，更多变换可以通过扩展接口实现。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.transform;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
