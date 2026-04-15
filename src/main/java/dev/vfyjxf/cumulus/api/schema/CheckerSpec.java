package dev.vfyjxf.cumulus.api.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

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
