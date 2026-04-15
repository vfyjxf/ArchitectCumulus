/**
 * common driver 实现集合。
 *
 * <p>driver 是“无事件流/事件不完整”时的主动扫描策略：
 * <ul>
 *     <li>仅已加载 section 的对账扫描</li>
 *     <li>对 INCOMPLETE 实例发出缺失 section 的请求（不加载）</li>
 *     <li>按兴趣集轮询实体变化</li>
 * </ul>
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.common.driver;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
