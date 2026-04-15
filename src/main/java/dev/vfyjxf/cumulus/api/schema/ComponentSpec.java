package dev.vfyjxf.cumulus.api.schema;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 一个组件描述：{@code type + config}。
 *
 * <p>config 以 {@link Dynamic} 表示，便于在 JSON/NBT 等不同 ops 间复用解析逻辑。
 */
public record ComponentSpec(ResourceLocation type, Dynamic<?> config) {
    public static final Dynamic<?> EMPTY_CONFIG = new Dynamic<>(JsonOps.INSTANCE, new JsonObject());

    public static final Codec<ComponentSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(ComponentSpec::type),
            Codec.PASSTHROUGH.optionalFieldOf("config", EMPTY_CONFIG).forGetter(ComponentSpec::config)
    ).apply(instance, ComponentSpec::new));

    public ComponentSpec {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(config, "config");
    }
}
