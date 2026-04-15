package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.CheckContext;

/**
 * checker：结构合法性检查。
 *
 * <p>checker 通常只读取 bindings 与必要的世界数据（并遵守 budget）。
 * 若缺数据，应返回 INCOMPLETE 并产出 DataRequest。
 */
public interface Checker {
    CheckReport check(CheckContext ctx);
}
