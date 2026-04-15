package dev.vfyjxf.cumulus.common.compiler;

import dev.vfyjxf.cumulus.api.binding.BindingsView;
import dev.vfyjxf.cumulus.api.context.CompileContext;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.pass.CompileReport;
import dev.vfyjxf.cumulus.api.pass.Compiler;
import dev.vfyjxf.cumulus.api.runtime.ContextKey;
import dev.vfyjxf.cumulus.api.runtime.MapRuntimeContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class BindingsCompiler implements Compiler {
    public static final ContextKey<BlockPos> ANCHOR_POS = new Key<>(ResourceLocation.fromNamespaceAndPath("cumulus", "anchor_pos"), BlockPos.class);
    public static final ContextKey<BindingsView> BINDINGS = new Key<>(ResourceLocation.fromNamespaceAndPath("cumulus", "bindings"), BindingsView.class);

    @Override
    public CompileReport compile(CompileContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        MapRuntimeContext out = new MapRuntimeContext();
        out.put(ANCHOR_POS, ctx.anchorPos());
        out.put(BINDINGS, ctx.bindings());
        return CompileReport.ok(out, InterestUpdate.empty());
    }

    public record Key<T>(ResourceLocation id, Class<T> valueType) implements ContextKey<T> {
        public Key {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(valueType, "valueType");
        }
    }
}

