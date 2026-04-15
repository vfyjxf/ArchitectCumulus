/**
 * 结构化诊断（非法结构分析）模型。
 *
 * <p>{@link dev.vfyjxf.cumulus.api.diagnostic.Diagnostic} 由各 pass 产生并汇总到实例快照中，
 * 以稳定错误码 + 证据 +（可选）trace/fixHints 的形式表达“为什么非法/缺数据/不可确定”。
 */
@NotNullByDefault
package dev.vfyjxf.cumulus.api.diagnostic;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
