package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.CompileContext;

public interface Compiler {
    CompileReport compile(CompileContext ctx);
}
