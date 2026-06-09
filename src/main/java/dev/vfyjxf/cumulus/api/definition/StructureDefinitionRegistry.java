package dev.vfyjxf.cumulus.api.definition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class StructureDefinitionRegistry {
    private final Map<ResourceLocation, StructureDefinition> definitionsById = new LinkedHashMap<>();

    public synchronized StructureDefinitionRegistry register(StructureDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        definitionsById.put(definition.id(), definition);
        return this;
    }

    public synchronized Optional<StructureDefinition> resolve(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Structure definition id must not be null");
        }
        return Optional.ofNullable(definitionsById.get(id));
    }

    public synchronized List<StructureDefinition> definitions() {
        return List.copyOf(new ArrayList<>(definitionsById.values()));
    }
}
