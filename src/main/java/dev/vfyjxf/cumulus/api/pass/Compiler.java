package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.CompileContext;

/**
 * compiler：把结构编译为运行时上下文（RuntimeContext）。
 *
 * <p>只有当结构合法且数据完整时，编译产物才应被认为可用。
 */
public interface Compiler {
    CompileReport compile(CompileContext ctx);
}
