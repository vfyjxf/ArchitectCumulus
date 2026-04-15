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

/**
 * 一个最小 compiler：把常用字段写入 {@link dev.vfyjxf.cumulus.api.runtime.RuntimeContext}。
 *
 * <p>写入：
 * <ul>
 *     <li>{@link #ANCHOR_POS}：anchor 坐标</li>
 *     <li>{@link #BINDINGS}：bindings 视图</li>
 * </ul>
 *
 * <p>生产场景通常会写入更结构化的上下文（例如预计算邻接关系、IO 口列表、方块实体引用等）。
 */
public final class BindingsCompiler implements Compiler {
    public static final ContextKey<BlockPos> ANCHOR_POS = new Key<>(ResourceLocation.fromNamespaceAndPath("cumulus", "anchor_pos"), BlockPos.class);
    public static final ContextKey<BindingsView> BINDINGS = new Key<>(ResourceLocation.fromNamespaceAndPath("cumulus", "bindings"), BindingsView.class);

    @Override
    public CompileReport compile(CompileContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        MapRuntimeContext out = new MapRuntimeContext();
        // 把基础信息塞进上下文，供后续 linker/机器逻辑消费。
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
