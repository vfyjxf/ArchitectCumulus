package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.CheckContext;

public interface Checker {
    CheckReport check(CheckContext ctx);
}
