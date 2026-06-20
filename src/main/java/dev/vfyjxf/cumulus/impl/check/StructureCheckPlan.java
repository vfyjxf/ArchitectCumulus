package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.definition.StructureDefinition;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record StructureCheckPlan(
                ResourceLocation definitionId,
                StructureDefinition definition,
                List<CheckPlanCandidate> candidates) {
    public StructureCheckPlan {
        if (definitionId == null) {
            throw new IllegalArgumentException("Check plan definition id must not be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("Check plan definition must not be null");
        }
        candidates = List.copyOf(candidates);
    }
}
