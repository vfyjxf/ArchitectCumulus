package dev.vfyjxf.cumulus.api.target.types;

import dev.vfyjxf.cumulus.api.target.TargetProperties;
import dev.vfyjxf.cumulus.api.target.TargetType;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Target category: an externally-injected context value (not a world target).
 * Carries the typed {@link ContextKey} this point reads from at check time.
 */
public final class ContextTargetType implements TargetType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:context");
    public static final ContextTargetType instance = new ContextTargetType();

    private ContextTargetType() {
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    /** Context target properties: the typed key this target resolves against. */
    public record Props(ContextKey<?> key) implements TargetProperties {
        public Props {
            Objects.requireNonNull(key, "Context key must not be null");
        }
    }
}
