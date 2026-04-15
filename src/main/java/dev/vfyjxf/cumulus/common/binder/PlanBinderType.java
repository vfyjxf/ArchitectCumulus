package dev.vfyjxf.cumulus.common.binder;

import com.mojang.serialization.Dynamic;
import dev.vfyjxf.cumulus.api.binding.plan.BindingPlan;
import dev.vfyjxf.cumulus.api.pass.Binder;
import dev.vfyjxf.cumulus.api.schema.SchemaBuildContext;
import dev.vfyjxf.cumulus.api.schema.SchemaComponentType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * {@code cumulus:plan_binder}：把 {@link BindingPlan}（JSON）解析成 {@link PlanBinder}。
 *
 * <p>config 结构：见 {@link BindingPlan#CODEC}。
 */
public final class PlanBinderType implements SchemaComponentType<Binder> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cumulus", "plan_binder");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Binder create(Dynamic<?> config, SchemaBuildContext ctx) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(ctx, "ctx");
        @SuppressWarnings("unchecked")
        Dynamic<Object> typed = (Dynamic<Object>) (Dynamic<?>) config;
        BindingPlan plan = BindingPlan.CODEC.parse(typed)
                .getOrThrow(message -> new IllegalArgumentException("Invalid plan binder config: " + message));
        return PlanBinder.of(plan);
    }
}
