package dev.vfyjxf.cumulus.api.engine;

import java.util.Objects;
import java.util.Optional;

public record InstanceDelta(InstanceKey key, InstanceDeltaKind kind, Optional<InstanceView> snapshot) {
    public InstanceDelta {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(kind, "kind");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public static InstanceDelta added(InstanceView snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new InstanceDelta(snapshot.key(), InstanceDeltaKind.ADDED, Optional.of(snapshot));
    }

    public static InstanceDelta updated(InstanceView snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new InstanceDelta(snapshot.key(), InstanceDeltaKind.UPDATED, Optional.of(snapshot));
    }

    public static InstanceDelta removed(InstanceKey key, Optional<InstanceView> lastSnapshot) {
        return new InstanceDelta(key, InstanceDeltaKind.REMOVED, lastSnapshot);
    }
}
