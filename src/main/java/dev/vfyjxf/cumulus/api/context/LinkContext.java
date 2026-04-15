package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

/**
 * linker 上下文：提供 compiledContext 访问（并可通过 instances() 读取依赖实例的 context）。
 */
public interface LinkContext extends PassContext {
    RuntimeContextView compiledContext();
}
