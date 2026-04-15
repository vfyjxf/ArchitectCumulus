package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.binding.BindingsView;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

/**
 * exporter 上下文：提供 bindings 与最终 runtimeContext 访问。
 */
public interface ExportContext extends PassContext {
    BindingsView bindings();

    RuntimeContextView runtimeContext();
}
