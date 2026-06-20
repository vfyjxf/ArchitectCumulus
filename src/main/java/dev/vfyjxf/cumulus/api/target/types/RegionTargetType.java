package dev.vfyjxf.cumulus.api.target.types;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.target.TargetProperties;
import dev.vfyjxf.cumulus.api.target.TargetType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Target category: a rectangular region (inclusive bounds). Used for "any block in
 * this AABB matches" predicates like door openings, clear-space requirements, or
 * fluid pools. Carries the region's inclusive from/to relative bounds.
 */
public final class RegionTargetType implements TargetType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:region");
    public static final RegionTargetType instance = new RegionTargetType();

    private RegionTargetType() {
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    /** Region target properties: inclusive from/to bounds. */
    public record Props(RelativeBlockPos from, RelativeBlockPos to) implements TargetProperties {
        public Props {
            Objects.requireNonNull(from, "from must not be null");
            Objects.requireNonNull(to, "to must not be null");
            if (from.x() > to.x() || from.y() > to.y() || from.z() > to.z()) {
                throw new IllegalArgumentException("Region bounds must be inclusive and non-inverted");
            }
        }
    }
}
