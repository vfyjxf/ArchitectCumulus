package dev.vfyjxf.cumulus.api.context;

/**
 * Typed identifier for a single child occurrence inside a parent structure's
 * runtime context. Replaces the raw {@code String} occurrence id used in legacy
 * keys like {@code "substructure.arm#0.status"}.
 *
 * <p>Format: the child definition name + {@code #} + a per-parent zero-based index,
 * matching the legacy string convention so the wire format stays unchanged.
 *
 * <pre>{@code
 * SubstructureOccurrenceId leftArm = SubstructureOccurrenceId.of("arm", 0);
 * // → "arm#0"
 * ContextKey<String> status = SubstructurePaths.status(leftArm);
 * // → ContextKey("substructure.arm#0.status")
 * }</pre>
 */
public record SubstructureOccurrenceId(String value) {

    public SubstructureOccurrenceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Substructure occurrence id must not be blank");
        }
    }

    public static SubstructureOccurrenceId of(String childDefName, int index) {
        if (childDefName == null || childDefName.isBlank()) {
            throw new IllegalArgumentException("Child definition name must not be blank");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Child occurrence index must not be negative");
        }
        return new SubstructureOccurrenceId(childDefName + "#" + index);
    }

    /** The child definition name portion before {@code #}. */
    public String childDefinitionName() {
        var hash = value.indexOf('#');
        return hash <= 0 ? value : value.substring(0, hash);
    }

    /** The numeric index portion after {@code #}, or {@code -1} if absent. */
    public int index() {
        var hash = value.indexOf('#');
        if (hash < 0 || hash == value.length() - 1) {
            return -1;
        }
        try {
            return Integer.parseInt(value.substring(hash + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
