package dev.vfyjxf.cumulus.impl.design3.runtime;

import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import java.util.List;

public record RuntimeFrameResult(
        int tick,
        List<StructureInstanceKey> checked,
        List<InstanceDelta> deltas,
        List<DataRequest> requests,
        int remainingQueued,
        List<RuntimeFastCheck> fastChecks) {
    public RuntimeFrameResult(
            int tick,
            List<StructureInstanceKey> checked,
            List<InstanceDelta> deltas,
            List<DataRequest> requests,
            int remainingQueued) {
        this(tick, checked, deltas, requests, remainingQueued, List.of());
    }

    public RuntimeFrameResult {
        checked = List.copyOf(checked);
        deltas = List.copyOf(deltas);
        requests = List.copyOf(requests);
        fastChecks = List.copyOf(fastChecks);
        if (remainingQueued < 0) {
            throw new IllegalArgumentException("Remaining queued count must not be negative");
        }
    }
}
