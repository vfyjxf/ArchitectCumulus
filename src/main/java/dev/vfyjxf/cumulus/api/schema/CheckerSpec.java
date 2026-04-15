package dev.vfyjxf.cumulus.api.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * 一个命名 checker 的数据描述。
 *
 * <p>{@link #id()} 是 checker 的逻辑名，用于调试/诊断；{@link #component()} 描述其类型与配置。
 */
public record CheckerSpec(String id, ComponentSpec component) {
    public static final Codec<CheckerSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CheckerSpec::id),
            ComponentSpec.CODEC.fieldOf("component").forGetter(CheckerSpec::component)
    ).apply(instance, CheckerSpec::new));

    public CheckerSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(component, "component");
    }
}
