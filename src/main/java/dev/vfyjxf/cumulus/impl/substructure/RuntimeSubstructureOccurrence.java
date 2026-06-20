package dev.vfyjxf.cumulus.impl.substructure;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;

import net.minecraft.resources.ResourceLocation;

public record RuntimeSubstructureOccurrence(
                String occurrenceId,
                ResourceLocation definitionId,
                String parentOccurrenceId,
                RelativeBlockPos anchorOffsetFromRoot) {
    public RuntimeSubstructureOccurrence {
        if (occurrenceId == null || occurrenceId.isBlank()) {
            throw new IllegalArgumentException("Substructure occurrence id must not be blank");
        }
        if (definitionId == null) {
            throw new IllegalArgumentException("Substructure definition id must not be null");
        }
        if (parentOccurrenceId == null || parentOccurrenceId.isBlank()) {
            throw new IllegalArgumentException("Parent occurrence id must not be blank");
        }
        if (anchorOffsetFromRoot == null) {
            throw new IllegalArgumentException("Substructure root-relative anchor offset must not be null");
        }
    }
}
