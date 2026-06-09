package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructureDefinitionRegistry;
import dev.vfyjxf.cumulus.impl.codec.RuntimeAssetRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public final class RuntimeAssetEngineBridge {
    private final RuntimeAssetRepository repository;
    private final RuntimeCheckEngine engine;
    private final Function<ResourceLocation, Optional<StructureDefinition>> definitionResolver;

    public RuntimeAssetEngineBridge(RuntimeAssetRepository repository, RuntimeCheckEngine engine) {
        this(repository, engine, id -> Optional.empty());
    }

    public RuntimeAssetEngineBridge(
            RuntimeAssetRepository repository,
            RuntimeCheckEngine engine,
            StructureDefinitionRegistry definitionRegistry) {
        this(repository, engine, requireRegistry(definitionRegistry)::resolve);
    }

    public RuntimeAssetEngineBridge(
            RuntimeAssetRepository repository,
            RuntimeCheckEngine engine,
            Function<ResourceLocation, Optional<StructureDefinition>> definitionResolver) {
        if (repository == null) {
            throw new IllegalArgumentException("Runtime asset repository must not be null");
        }
        if (engine == null) {
            throw new IllegalArgumentException("Runtime check engine must not be null");
        }
        if (definitionResolver == null) {
            throw new IllegalArgumentException("Definition resolver must not be null");
        }
        this.repository = repository;
        this.engine = engine;
        this.definitionResolver = definitionResolver;
    }

    private static StructureDefinitionRegistry requireRegistry(StructureDefinitionRegistry definitionRegistry) {
        if (definitionRegistry == null) {
            throw new IllegalArgumentException("Structure definition registry must not be null");
        }
        return definitionRegistry;
    }

    public List<ResourceLocation> registerDefinitions() {
        var registered = new ArrayList<ResourceLocation>();
        for (var asset : repository.assets()) {
            var definition = asset.definition()
                    .or(() -> definitionResolver.apply(asset.definitionId()));
            definition.ifPresent(value -> {
                engine.register(value);
                registered.add(value.id());
            });
        }
        return List.copyOf(registered);
    }
}
