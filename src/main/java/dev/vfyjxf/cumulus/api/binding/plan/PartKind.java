package dev.vfyjxf.cumulus.api.binding.plan;

import com.mojang.serialization.Codec;

import java.util.Locale;

/**
 * 绑定计划中可扫描的部件类型。
 */
public enum PartKind {
    BLOCK,
    BLOCK_ENTITY,
    ENTITY
    ;

    public static final Codec<PartKind> CODEC = Codec.STRING.xmap(
            value -> PartKind.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT)
    );
}
