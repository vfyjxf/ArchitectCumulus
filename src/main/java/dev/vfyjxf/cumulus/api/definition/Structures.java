package dev.vfyjxf.cumulus.api.definition;

import net.minecraft.resources.ResourceLocation;

public final class Structures {
    private Structures() {
    }

    public static StructureDefinitionBuilder define(ResourceLocation id) {
        return new StructureDefinitionBuilder(id);
    }
}
