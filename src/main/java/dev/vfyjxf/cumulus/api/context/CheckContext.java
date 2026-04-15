package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.binding.BindingsView;

/**
 * checker 上下文：在 {@link PassContext} 基础上提供 bindings 访问。
 */
public interface CheckContext extends PassContext {
    BindingsView bindings();
}
