package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.BindContext;

public interface Binder {
    BindReport bind(BindContext ctx);
}
