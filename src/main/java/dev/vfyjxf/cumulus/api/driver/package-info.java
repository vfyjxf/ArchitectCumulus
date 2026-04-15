/**
 * 主动扫描策略（drivers）。
 *
 * <p>当 Minecraft 环境没有完整事件流时，driver 用于按预算（budget）主动产出 dirty sections/anchors，
 * 或对 {@code INCOMPLETE} 实例生成缺失 section 的 {@link dev.vfyjxf.cumulus.api.request.DataRequest}。
 *
 * <p>driver 的输出是“提示/建议”，引擎可将其与外部 {@link dev.vfyjxf.cumulus.api.engine.ChangeSet} 合并后执行。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.driver;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
