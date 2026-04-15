package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.binding.BindingsView;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

/**
 * 依赖声明上下文：在 {@link PassContext} 基础上提供 bindings 与 compiledContext 访问。
 */
public interface DepContext extends PassContext {
    BindingsView bindings();

    RuntimeContextView compiledContext();
}
