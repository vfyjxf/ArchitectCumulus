package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.request.DataRequest;

import java.util.List;
import java.util.Objects;

public record EngineFrame(List<InstanceDelta> deltas, List<DataRequest> newRequests) {
    public EngineFrame {
        deltas = List.copyOf(Objects.requireNonNull(deltas, "deltas"));
        newRequests = List.copyOf(Objects.requireNonNull(newRequests, "newRequests"));
    }

    public static EngineFrame empty() {
        return new EngineFrame(List.of(), List.of());
    }
}
