package dev.vfyjxf.cumulus.api.schema;

import com.mojang.serialization.Dynamic;
import net.minecraft.resources.ResourceLocation;

public interface SchemaComponentType<T> {
    ResourceLocation id();

    T create(Dynamic<?> config, SchemaBuildContext ctx);
}

