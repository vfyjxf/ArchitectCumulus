package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.BindContext;

/**
 * binder：围绕 anchor 扫描世界并生成 bindings。
 *
 * <p>建议 binder 同时：
 * <ul>
 *     <li>产出 bindings（role -> parts）</li>
 *     <li>声明兴趣集（InterestUpdate.sectionsAdd 等），用于后续增量轮询</li>
 *     <li>遇到缺数据时返回 INCOMPLETE + DataRequest</li>
 * </ul>
 */
public interface Binder {
    BindReport bind(BindContext ctx);
}
