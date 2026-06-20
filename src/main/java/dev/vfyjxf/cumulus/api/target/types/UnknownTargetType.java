package dev.vfyjxf.cumulus.api.target.types;

import dev.vfyjxf.cumulus.api.target.TargetProperties;
import dev.vfyjxf.cumulus.api.target.TargetType;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;

/**
 * Fallback target type used when the codec encounters an id not registered in
 * {@link dev.vfyjxf.cumulus.api.target.TargetTypes}. Preserves the original id
 * so consumers can report it in diagnostics and so a future mod load can
 * resolve it correctly.
 *
 * <p>Not registered in {@code TargetTypes} — instances are created on demand
 * by the codec via {@link #of(ResourceLocation)}.
 */
public final class UnknownTargetType implements TargetType {

    private final ResourceLocation id;

    private UnknownTargetType(ResourceLocation id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public static UnknownTargetType of(ResourceLocation id) {
        return new UnknownTargetType(id);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    /**
     * Unknown targets carry the raw property map verbatim — no typed access
     * is possible until a real TargetType is registered.
     */
    public record Props(Map<String, String> raw) implements TargetProperties {
        public Props {
            Objects.requireNonNull(raw, "raw properties map must not be null");
            raw = Map.copyOf(raw);
        }
    }
}
