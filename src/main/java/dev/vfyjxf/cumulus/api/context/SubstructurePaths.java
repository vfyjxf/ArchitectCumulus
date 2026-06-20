package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.types.ContextKey;

import java.util.Optional;

/**
 * Centralized builder/parser for context keys that mirror substructure state
 * back into the parent {@code RuntimeStructureContext}.
 *
 * <p>Replaces the legacy scattered string concatenation patterns like
 * {@code "substructure." + occ + ".context." + key}. The string format is
 * preserved bit-for-bit so existing datapack JSON and serialized contexts
 * remain valid.
 *
 * <h2>Producing keys (modder / runtime)</h2>
 * <pre>{@code
 * SubstructureOccurrenceId leftArm = SubstructureOccurrenceId.of("arm", 0);
 * ContextKey<String> status   = SubstructurePaths.status(leftArm);
 * ContextKey<String> offset   = SubstructurePaths.anchorOffset(leftArm);
 * ContextKey<String> toolId   = SubstructurePaths.context(leftArm, "tool.spell_book");
 * }</pre>
 *
 * <h2>Inspecting keys (consumers)</h2>
 * <pre>{@code
 * SubstructurePathInspector inspector = SubstructurePaths.inspect(key);
 * if (inspector.isSubstructure()) {
 *     SubstructureOccurrenceId occ = inspector.occurrence().orElseThrow();
 *     // ...
 * }
 * }</pre>
 */
public final class SubstructurePaths {

    public static final String prefix = "substructure.";

    private SubstructurePaths() {}

    public static ContextKey<String> status(SubstructureOccurrenceId occurrence) {
        return ContextKey.string(prefix + occurrence.value() + ".status");
    }

    public static ContextKey<String> anchorOffset(SubstructureOccurrenceId occurrence) {
        return ContextKey.string(prefix + occurrence.value() + ".anchor_offset");
    }

    /**
     * Build a key that mirrors a child capture into the parent context.
     * Mirrors the legacy {@code "substructure.<occ>.context.<capturePath>"} format.
     */
    public static ContextKey<String> context(SubstructureOccurrenceId occurrence, String capturePath) {
        if (capturePath == null || capturePath.isBlank()) {
            throw new IllegalArgumentException("Capture path must not be blank");
        }
        return ContextKey.string(prefix + occurrence.value() + ".context." + capturePath);
    }

    /**
     * Parse a key name back into its structured form, if it is a substructure path.
     * Returns empty for ordinary keys (callers should check {@link InspectResult#isSubstructure()}).
     */
    public static InspectResult inspect(ContextKey<?> key) {
        var name = key.name();
        if (!name.startsWith(prefix)) {
            return InspectResult.notSubstructure();
        }
        var rest = name.substring(prefix.length());

        var contextIdx = rest.indexOf(".context.");
        if (contextIdx > 0) {
            var occPart = rest.substring(0, contextIdx);
            var capturePath = rest.substring(contextIdx + ".context.".length());
            return new InspectResult(
                            Optional.of(new SubstructureOccurrenceId(occPart)),
                            Optional.of(capturePath),
                            Optional.empty());
        }

        var lastDot = rest.lastIndexOf('.');
        if (lastDot <= 0) {
            return InspectResult.notSubstructure();
        }
        var occPart = rest.substring(0, lastDot);
        var field = rest.substring(lastDot + 1);
        var fieldKind = switch (field) {
            case "status" -> Optional.of(Field.status);
            case "anchor_offset" -> Optional.of(Field.anchorOffset);
            default -> Optional.<Field>empty();
        };
        if (fieldKind.isEmpty()) {
            return InspectResult.notSubstructure();
        }
        return new InspectResult(
                        Optional.of(new SubstructureOccurrenceId(occPart)),
                        Optional.empty(),
                        fieldKind);
    }

    public enum Field {
        status, anchorOffset
    }

    /**
     * Result of {@link #inspect(ContextKey)}. Either non-substructure, a
     * status/anchor_offset path, or a context mirroring path.
     */
    public record InspectResult(
                    Optional<SubstructureOccurrenceId> occurrence,
                    Optional<String> capturePath,
                    Optional<Field> field) {

        public boolean isSubstructure() {
            return occurrence.isPresent();
        }

        public boolean isContextMirror() {
            return occurrence.isPresent() && capturePath.isPresent();
        }

        public boolean isFieldMirror() {
            return occurrence.isPresent() && field.isPresent();
        }

        public static InspectResult notSubstructure() {
            return new InspectResult(Optional.empty(), Optional.empty(), Optional.empty());
        }
    }
}
