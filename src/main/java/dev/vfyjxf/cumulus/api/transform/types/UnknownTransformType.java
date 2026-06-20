package dev.vfyjxf.cumulus.api.transform.types;

import dev.vfyjxf.cumulus.api.transform.TransformProperties;
import dev.vfyjxf.cumulus.api.transform.TransformType;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;

/**
 * Fallback transform type for ids not registered in
 * {@link dev.vfyjxf.cumulus.api.transform.TransformTypes}.
 */
public final class UnknownTransformType implements TransformType {

    private final ResourceLocation id;

    private UnknownTransformType(ResourceLocation id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public static UnknownTransformType of(ResourceLocation id) {
        return new UnknownTransformType(id);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TransformProperties> propertiesType() {
        return Props.class;
    }

    public record Props(Map<String, String> raw) implements TransformProperties {
        public Props {
            Objects.requireNonNull(raw, "raw must not be null");
            raw = Map.copyOf(raw);
        }
    }
}
