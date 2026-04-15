package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.export.ExportView;
import dev.vfyjxf.cumulus.api.interest.InterestView;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 默认的实例快照实现。
 *
 * <p>实现方也可以提供自定义 {@link InstanceView}，但建议保持字段含义一致。
 */
public record InstanceSnapshot(
        InstanceKey key,
        InstanceStatus status,
        List<Diagnostic> diagnostics,
        Optional<RuntimeContextView> runtimeContext,
        Optional<ExportView> exportView,
        InterestView interest
) implements InstanceView {
    public InstanceSnapshot {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        runtimeContext = Objects.requireNonNull(runtimeContext, "runtimeContext");
        exportView = Objects.requireNonNull(exportView, "exportView");
        Objects.requireNonNull(interest, "interest");
    }
}
