package dev.vfyjxf.cumulus.api.schema;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ArchitectSchemaSpec(
        ResourceLocation id,
        ComponentSpec anchorFinder,
        ComponentSpec binder,
        List<CheckerSpec> checkers,
        ComponentSpec compiler,
        Optional<ComponentSpec> dependencyDeclarer,
        Optional<ComponentSpec> linker,
        Optional<ComponentSpec> exporter
) {
    public static final Dynamic<?> EMPTY_CONFIG = new Dynamic<>(JsonOps.INSTANCE, new JsonObject());

    public static final Codec<ArchitectSchemaSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ArchitectSchemaSpec::id),
            ComponentSpec.CODEC.fieldOf("anchor_finder").forGetter(ArchitectSchemaSpec::anchorFinder),
            ComponentSpec.CODEC.fieldOf("binder").forGetter(ArchitectSchemaSpec::binder),
            CheckerSpec.CODEC.listOf().optionalFieldOf("checkers", List.of()).forGetter(ArchitectSchemaSpec::checkers),
            ComponentSpec.CODEC.fieldOf("compiler").forGetter(ArchitectSchemaSpec::compiler),
            ComponentSpec.CODEC.optionalFieldOf("dependency_declarer").forGetter(ArchitectSchemaSpec::dependencyDeclarer),
            ComponentSpec.CODEC.optionalFieldOf("linker").forGetter(ArchitectSchemaSpec::linker),
            ComponentSpec.CODEC.optionalFieldOf("exporter").forGetter(ArchitectSchemaSpec::exporter)
    ).apply(instance, ArchitectSchemaSpec::new));

    public ArchitectSchemaSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(anchorFinder, "anchorFinder");
        Objects.requireNonNull(binder, "binder");
        checkers = List.copyOf(Objects.requireNonNull(checkers, "checkers"));
        Objects.requireNonNull(compiler, "compiler");
        dependencyDeclarer = Objects.requireNonNull(dependencyDeclarer, "dependencyDeclarer");
        linker = Objects.requireNonNull(linker, "linker");
        exporter = Objects.requireNonNull(exporter, "exporter");
    }

    public static ArchitectSchemaSpec minimal(ResourceLocation id, ComponentSpec anchorFinder, ComponentSpec binder, ComponentSpec compiler) {
        return new ArchitectSchemaSpec(id, anchorFinder, binder, List.of(), compiler, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ComponentSpec emptyConfig(ResourceLocation type) {
        return new ComponentSpec(type, EMPTY_CONFIG);
    }
}
