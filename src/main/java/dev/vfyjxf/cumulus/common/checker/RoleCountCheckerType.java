package dev.vfyjxf.cumulus.common.checker;

import com.mojang.serialization.Dynamic;
import dev.vfyjxf.cumulus.api.pass.Checker;
import dev.vfyjxf.cumulus.api.schema.SchemaBuildContext;
import dev.vfyjxf.cumulus.api.schema.SchemaComponentType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * {@code cumulus:role_count_checker}。
 *
 * <p>config 示例：
 * <pre>{@code
 * { "role": "controller", "min": 1, "max": 1 }
 * }</pre>
 */
public final class RoleCountCheckerType implements SchemaComponentType<Checker> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cumulus", "role_count_checker");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Checker create(Dynamic<?> config, SchemaBuildContext ctx) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(ctx, "ctx");
        String role = config.get("role").asString()
                .getOrThrow(message -> new IllegalArgumentException("Invalid role_count_checker.role: " + message));
        int min = config.get("min").asInt(0);
        int max = config.get("max").asInt(Integer.MAX_VALUE);
        return new RoleCountChecker(role, min, max);
    }
}
