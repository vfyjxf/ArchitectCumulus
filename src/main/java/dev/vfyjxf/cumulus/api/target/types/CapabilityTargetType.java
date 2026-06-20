package dev.vfyjxf.cumulus.api.target.types;

import dev.vfyjxf.cumulus.api.target.TargetProperties;
import dev.vfyjxf.cumulus.api.target.TargetType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Target category: a NeoForge block capability (energy/item/fluid/custom).
 * Carries the capability name (e.g. {@code "cumulus:energy"}).
 */
public final class CapabilityTargetType implements TargetType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:capability");
    public static final CapabilityTargetType instance = new CapabilityTargetType();

    private CapabilityTargetType() {
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    /** Capability target properties: the capability name (registry id). */
    public record Props(String capabilityName) implements TargetProperties {
        public Props {
            if (capabilityName == null || capabilityName.isBlank()) {
                throw new IllegalArgumentException("capabilityName must not be blank");
            }
        }
    }
}
