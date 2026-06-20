package dev.vfyjxf.cumulus.impl;

import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;

import java.util.List;

public record EngineFrame(
                List<InstanceDelta> deltas,
                List<DataRequest> requests) {
    public EngineFrame {
        deltas = List.copyOf(deltas);
        requests = List.copyOf(requests);
    }
}
