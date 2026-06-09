package dev.vfyjxf.cumulus.impl.design3.substructure;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import net.minecraft.resources.ResourceLocation;

public record SubstructureGraphNode(
        String occurrenceId,
        ResourceLocation definitionId,
        String parentOccurrenceId,
        String componentName,
        RelativeBlockPos anchorOffsetFromParent,
        int depth) {
    public SubstructureGraphNode {
        if (occurrenceId == null || occurrenceId.isBlank()) {
            throw new IllegalArgumentException("Substructure occurrence id must not be blank");
        }
        if (definitionId == null) {
            throw new IllegalArgumentException("Substructure definition id must not be null");
        }
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("Substructure component name must not be blank");
        }
        if (anchorOffsetFromParent == null) {
            throw new IllegalArgumentException("Substructure anchor offset must not be null");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("Substructure depth must not be negative");
        }
    }
}
