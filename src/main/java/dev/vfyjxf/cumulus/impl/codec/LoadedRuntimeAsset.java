package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record LoadedRuntimeAsset(
        String resourcePath,
        ResourceLocation id,
        int schemaVersion,
        ResourceLocation definitionId,
        Optional<StructureDefinition> definition,
        boolean debugAsset,
        StructureRuntimePlan runtimePlan,
        List<PredicateDescriptor> predicates) {
    public LoadedRuntimeAsset {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Loaded runtime asset path must not be blank");
        }
        if (id == null) {
            throw new IllegalArgumentException("Loaded runtime asset id must not be null");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Loaded runtime asset schema version must be positive");
        }
        if (definitionId == null) {
            throw new IllegalArgumentException("Loaded runtime asset definition id must not be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("Loaded runtime asset definition optional must not be null");
        }
        definition.ifPresent(value -> {
            if (!value.id().equals(definitionId)) {
                throw new IllegalArgumentException("Loaded runtime asset embedded definition id must match definition id");
            }
        });
        if (runtimePlan == null) {
            throw new IllegalArgumentException("Loaded runtime asset plan must not be null");
        }
        if (!runtimePlan.definitionId().equals(definitionId)) {
            throw new IllegalArgumentException("Loaded runtime asset runtime plan definition id must match definition id");
        }
        predicates = List.copyOf(predicates);
    }

    public LoadedRuntimeAsset(
            String resourcePath,
            ResourceLocation id,
            int schemaVersion,
            ResourceLocation definitionId,
            boolean debugAsset,
            StructureRuntimePlan runtimePlan,
            List<PredicateDescriptor> predicates) {
        this(resourcePath, id, schemaVersion, definitionId, Optional.empty(), debugAsset, runtimePlan, predicates);
    }
}
