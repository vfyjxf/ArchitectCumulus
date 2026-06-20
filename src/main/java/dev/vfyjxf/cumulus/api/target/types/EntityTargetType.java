package dev.vfyjxf.cumulus.api.target.types;

import dev.vfyjxf.cumulus.api.target.TargetProperties;
import dev.vfyjxf.cumulus.api.target.TargetType;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Target category: a Minecraft entity. Carries the expected entity type id and
 * optional NBT snippet.
 */
public final class EntityTargetType implements TargetType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:entity");
    public static final EntityTargetType instance = new EntityTargetType();

    private EntityTargetType() {
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    /** Entity target properties: entity id and NBT snippet. */
    public record Props(
            ResourceLocation entityId,
            String nbt) implements TargetProperties {

        public Props {
            Objects.requireNonNull(entityId, "entityId must not be null");
            Objects.requireNonNull(nbt, "nbt must not be null");
        }
    }
}
