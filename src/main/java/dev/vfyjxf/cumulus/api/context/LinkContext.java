package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

public interface LinkContext extends PassContext {
    RuntimeContextView compiledContext();
}
