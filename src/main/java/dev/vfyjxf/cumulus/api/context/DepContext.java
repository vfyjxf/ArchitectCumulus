package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.binding.BindingsView;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

public interface DepContext extends PassContext {
    BindingsView bindings();

    RuntimeContextView compiledContext();
}
