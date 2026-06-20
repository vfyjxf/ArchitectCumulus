package dev.vfyjxf.cumulus.api.target.types;

import dev.vfyjxf.cumulus.api.target.TargetProperties;
import dev.vfyjxf.cumulus.api.target.TargetType;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Target category: a Minecraft block entity. Carries the expected block id,
 * block entity type id, and an optional NBT snippet.
 */
public final class BlockEntityTargetType implements TargetType {

    public static final ResourceLocation id = ResourceLocation.parse("cumulus:block_entity");
    public static final BlockEntityTargetType instance = new BlockEntityTargetType();

    private BlockEntityTargetType() {}

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends TargetProperties> propertiesType() {
        return Props.class;
    }

    /** Block entity target properties: block id, BE type id, NBT snippet. */
    public record Props(
                    ResourceLocation blockId,
                    ResourceLocation blockEntityId,
                    String nbt) implements TargetProperties {

        public Props {
            Objects.requireNonNull(blockId, "blockId must not be null");
            Objects.requireNonNull(blockEntityId, "blockEntityId must not be null");
            Objects.requireNonNull(nbt, "nbt must not be null");
        }
    }
}
