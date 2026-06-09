package dev.vfyjxf.cumulus.api.runtime;

public record InstanceDelta(
        StructureInstanceKey key,
        InstanceStatus status,
        RuntimeStructureContext context) {
    public InstanceDelta {
        if (key == null) {
            throw new IllegalArgumentException("Instance key must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Instance status must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Instance context must not be null");
        }
        if (!key.equals(context.key())) {
            throw new IllegalArgumentException("Instance delta key must match context key");
        }
    }
}
