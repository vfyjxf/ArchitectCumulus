package dev.vfyjxf.cumulus.api.definition;

import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

public final class Structures {
    private static final StructureDefinitionRegistry GLOBAL_REGISTRY = new StructureDefinitionRegistry();

    private Structures() {
    }

    public static StructureDefinitionBuilder define(ResourceLocation id) {
        return new StructureDefinitionBuilder(id);
    }

    public static StructureDefinitionRegistry registry() {
        return GLOBAL_REGISTRY;
    }

    public static StructureDefinitionRegistry register(StructureDefinition definition) {
        return GLOBAL_REGISTRY.register(definition);
    }

    public static Optional<StructureDefinition> resolve(ResourceLocation id) {
        return GLOBAL_REGISTRY.resolve(id);
    }
}
