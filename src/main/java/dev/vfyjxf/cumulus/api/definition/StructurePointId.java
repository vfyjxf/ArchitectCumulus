package dev.vfyjxf.cumulus.api.definition;

public record StructurePointId(String value) {
    public StructurePointId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Structure point id must not be blank");
        }
    }
}
