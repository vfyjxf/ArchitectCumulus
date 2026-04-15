package dev.vfyjxf.cumulus.common.schema;

import dev.vfyjxf.cumulus.common.binder.PlanBinderType;
import dev.vfyjxf.cumulus.common.anchor.MatchBlockAnchorFinderType;
import dev.vfyjxf.cumulus.common.checker.RoleCountCheckerType;
import dev.vfyjxf.cumulus.common.compiler.BindingsCompilerType;

public final class BuiltInSchemaTypes {
    private BuiltInSchemaTypes() {
    }

    public static void registerAll(SimpleSchemaTypeRegistry registry) {
        registry.registerAnchorFinder(new MatchBlockAnchorFinderType());
        registry.registerBinder(new PlanBinderType());
        registry.registerChecker(new RoleCountCheckerType());
        registry.registerCompiler(new BindingsCompilerType());
    }
}
