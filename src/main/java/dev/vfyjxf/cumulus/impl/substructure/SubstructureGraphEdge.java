package dev.vfyjxf.cumulus.impl.substructure;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import net.minecraft.resources.ResourceLocation;

public record SubstructureGraphEdge(
        String parentOccurrenceId,
        String childOccurrenceId,
        String componentName,
        ResourceLocation childDefinitionId,
        RelativeBlockPos anchorOffset) {
    public SubstructureGraphEdge {
        if (parentOccurrenceId == null || parentOccurrenceId.isBlank()) {
            throw new IllegalArgumentException("Substructure parent occurrence id must not be blank");
        }
        if (childOccurrenceId == null || childOccurrenceId.isBlank()) {
            throw new IllegalArgumentException("Substructure child occurrence id must not be blank");
        }
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("Substructure component name must not be blank");
        }
        if (childDefinitionId == null) {
            throw new IllegalArgumentException("Substructure child definition id must not be null");
        }
        if (anchorOffset == null) {
            throw new IllegalArgumentException("Substructure anchor offset must not be null");
        }
    }
}
