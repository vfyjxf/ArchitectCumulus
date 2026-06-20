package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Extra diagnostic metadata attached to structure points and components.
 * Carried through to {@link DiagnosticNode} generation so diagnostics can
 * include context-specific information (e.g. "tier", "facing", custom labels).
 *
 * <pre>{@code
 * DiagnosticMetadata.of("tier", "advanced", "facing", "north")
 * DiagnosticMetadata.builder().put("coil_type", "cupronickel").build()
 * }</pre>
 */
public record DiagnosticMetadata(Map<String, String> properties) {
    public DiagnosticMetadata {
        properties = Map.copyOf(properties);
    }

    public static DiagnosticMetadata empty() {
        return new DiagnosticMetadata(Map.of());
    }

    /** Create from a single key-value pair. */
    public static DiagnosticMetadata of(String k1, String v1) {
        return new DiagnosticMetadata(Map.of(k1, v1));
    }

    /** Create from two key-value pairs. */
    public static DiagnosticMetadata of(String k1, String v1, String k2, String v2) {
        var m = new LinkedHashMap<String, String>();
        m.put(k1, v1);
        m.put(k2, v2);
        return new DiagnosticMetadata(m);
    }

    /** Start a fluent builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** True if no properties are set. */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /** Get a property value, or empty. */
    public Optional<String> get(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /** Fluent builder for constructing metadata with arbitrary key-value pairs. */
    public static final class Builder {
        private final Map<String, String> map = new LinkedHashMap<>();

        public Builder put(String key, String value) {
            map.put(key, value);
            return this;
        }

        public DiagnosticMetadata build() {
            return new DiagnosticMetadata(map);
        }
    }
}
