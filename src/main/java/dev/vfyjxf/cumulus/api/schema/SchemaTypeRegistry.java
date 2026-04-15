package dev.vfyjxf.cumulus.api.schema;

import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.pass.Binder;
import dev.vfyjxf.cumulus.api.pass.Checker;
import dev.vfyjxf.cumulus.api.pass.Compiler;
import dev.vfyjxf.cumulus.api.pass.DependencyDeclarer;
import dev.vfyjxf.cumulus.api.pass.Exporter;
import dev.vfyjxf.cumulus.api.pass.Linker;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface SchemaTypeRegistry {
    Optional<SchemaComponentType<AnchorFinder>> anchorFinder(ResourceLocation type);

    Optional<SchemaComponentType<Binder>> binder(ResourceLocation type);

    Optional<SchemaComponentType<Checker>> checker(ResourceLocation type);

    Optional<SchemaComponentType<Compiler>> compiler(ResourceLocation type);

    Optional<SchemaComponentType<DependencyDeclarer>> dependencyDeclarer(ResourceLocation type);

    Optional<SchemaComponentType<Linker>> linker(ResourceLocation type);

    Optional<SchemaComponentType<Exporter>> exporter(ResourceLocation type);
}

