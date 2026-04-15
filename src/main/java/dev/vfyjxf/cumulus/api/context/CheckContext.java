package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.binding.BindingsView;

public interface CheckContext extends PassContext {
    BindingsView bindings();
}
