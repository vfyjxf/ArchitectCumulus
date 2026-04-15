package dev.vfyjxf.cumulus.api.engine;

import java.util.Objects;
import java.util.Optional;

/**
 * 实例变更（delta）。
 *
 * <p>当实例新增/更新/移除时，引擎会输出一个 delta。对于 {@link InstanceDeltaKind#REMOVED}，
 * {@link #snapshot()} 可能为空（实现可选择携带最后快照便于调试）。
 */
public record InstanceDelta(InstanceKey key, InstanceDeltaKind kind, Optional<InstanceView> snapshot) {
    public InstanceDelta {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(kind, "kind");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    /** 新增实例。 */
    public static InstanceDelta added(InstanceView snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new InstanceDelta(snapshot.key(), InstanceDeltaKind.ADDED, Optional.of(snapshot));
    }

    /** 更新实例。 */
    public static InstanceDelta updated(InstanceView snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new InstanceDelta(snapshot.key(), InstanceDeltaKind.UPDATED, Optional.of(snapshot));
    }

    /** 移除实例。 */
    public static InstanceDelta removed(InstanceKey key, Optional<InstanceView> lastSnapshot) {
        return new InstanceDelta(key, InstanceDeltaKind.REMOVED, lastSnapshot);
    }
}
