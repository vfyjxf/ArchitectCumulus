package dev.vfyjxf.cumulus.impl.check;

import java.util.Map;

public record SnapshotTarget(
        String value,
        Map<String, String> properties) {
    public SnapshotTarget {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Snapshot target value must not be blank");
        }
        properties = Map.copyOf(properties);
    }
}
