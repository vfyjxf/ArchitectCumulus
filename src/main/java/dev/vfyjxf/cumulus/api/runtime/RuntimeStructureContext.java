package dev.vfyjxf.cumulus.api.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public record RuntimeStructureContext(
        StructureInstanceKey key,
        int revision,
        Map<String, String> values) {
    public RuntimeStructureContext {
        if (key == null) {
            throw new IllegalArgumentException("Context key must not be null");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Context revision must not be negative");
        }
        values = Map.copyOf(values);
    }

    public static Builder builder(StructureInstanceKey key) {
        return new Builder(key);
    }

    public static final class Builder {
        private final StructureInstanceKey key;
        private int revision;
        private final Map<String, String> values = new LinkedHashMap<>();

        private Builder(StructureInstanceKey key) {
            if (key == null) {
                throw new IllegalArgumentException("Context key must not be null");
            }
            this.key = key;
        }

        public Builder revision(int revision) {
            this.revision = revision;
            return this;
        }

        public Builder put(String name, String value) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Context value name must not be blank");
            }
            if (value == null) {
                throw new IllegalArgumentException("Context value must not be null");
            }
            values.put(name, value);
            return this;
        }

        public RuntimeStructureContext build() {
            return new RuntimeStructureContext(key, revision, values);
        }
    }
}
