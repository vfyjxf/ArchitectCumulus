package dev.vfyjxf.cumulus.common.anchor;

import com.mojang.serialization.Dynamic;
import dev.vfyjxf.cumulus.api.binding.plan.MatchSpec;
import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.schema.SchemaBuildContext;
import dev.vfyjxf.cumulus.api.schema.SchemaComponentType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * {@code cumulus:match_block_anchor_finder}。
 *
 * <p>config 使用 {@link MatchSpec#CODEC}，含义为“哪些方块可作为 anchor”。
 */
public final class MatchBlockAnchorFinderType implements SchemaComponentType<AnchorFinder> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cumulus", "match_block_anchor_finder");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public AnchorFinder create(Dynamic<?> config, SchemaBuildContext ctx) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(ctx, "ctx");
        @SuppressWarnings("unchecked")
        Dynamic<Object> typed = (Dynamic<Object>) (Dynamic<?>) config;
        MatchSpec match = MatchSpec.CODEC.parse(typed)
                .getOrThrow(message -> new IllegalArgumentException("Invalid match_block_anchor_finder config: " + message));
        return new MatchBlockAnchorFinder(match);
    }
}
