package dev.vfyjxf.cumulus.api.definition;

public class StructureDefinitionException extends RuntimeException {
    public StructureDefinitionException(String message) {
        super(message);
    }

    public StructureDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
