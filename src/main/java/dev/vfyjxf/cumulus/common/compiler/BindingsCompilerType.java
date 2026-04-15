package dev.vfyjxf.cumulus.common.compiler;

import com.mojang.serialization.Dynamic;
import dev.vfyjxf.cumulus.api.pass.Compiler;
import dev.vfyjxf.cumulus.api.schema.SchemaBuildContext;
import dev.vfyjxf.cumulus.api.schema.SchemaComponentType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * {@code cumulus:bindings_compiler}：无配置的最小 compiler。
 */
public final class BindingsCompilerType implements SchemaComponentType<Compiler> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cumulus", "bindings_compiler");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Compiler create(Dynamic<?> config, SchemaBuildContext ctx) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(ctx, "ctx");
        return new BindingsCompiler();
    }
}
