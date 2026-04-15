package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.binding.BindingsView;

public interface CompileContext extends PassContext {
    BindingsView bindings();
}
