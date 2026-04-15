package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.binding.BindingsView;

/**
 * compiler 上下文：在 {@link PassContext} 基础上提供 bindings 访问。
 */
public interface CompileContext extends PassContext {
    BindingsView bindings();
}
