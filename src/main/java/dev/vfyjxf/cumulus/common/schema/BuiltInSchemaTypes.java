package dev.vfyjxf.cumulus.common.schema;

import dev.vfyjxf.cumulus.common.binder.PlanBinderType;
import dev.vfyjxf.cumulus.common.anchor.MatchBlockAnchorFinderType;
import dev.vfyjxf.cumulus.common.checker.RoleCountCheckerType;
import dev.vfyjxf.cumulus.common.compiler.BindingsCompilerType;

public final class BuiltInSchemaTypes {
    private BuiltInSchemaTypes() {
    }

    /**
     * 注册 common 内置的组件 type。
     *
     * <p>这些 type 主要用于演示/测试，让 JSON spec 可以在没有 datapack 的情况下先跑通：
     * <ul>
     *     <li>{@code cumulus:match_block_anchor_finder}</li>
     *     <li>{@code cumulus:plan_binder}</li>
     *     <li>{@code cumulus:role_count_checker}</li>
     *     <li>{@code cumulus:bindings_compiler}</li>
     * </ul>
     */
    public static void registerAll(SimpleSchemaTypeRegistry registry) {
        registry.registerAnchorFinder(new MatchBlockAnchorFinderType());
        registry.registerBinder(new PlanBinderType());
        registry.registerChecker(new RoleCountCheckerType());
        registry.registerCompiler(new BindingsCompilerType());
    }
}
