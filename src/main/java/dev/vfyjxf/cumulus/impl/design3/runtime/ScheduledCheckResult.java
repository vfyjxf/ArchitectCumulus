package dev.vfyjxf.cumulus.impl.design3.runtime;

import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.runtime.InstanceDelta;
import java.util.List;

public record ScheduledCheckResult(
        List<InstanceDelta> deltas,
        List<DataRequest> requests,
        List<RuntimeFastCheck> fastChecks) {
    public ScheduledCheckResult(
            List<InstanceDelta> deltas,
            List<DataRequest> requests) {
        this(deltas, requests, List.of());
    }

    public ScheduledCheckResult {
        deltas = List.copyOf(deltas);
        requests = List.copyOf(requests);
        fastChecks = List.copyOf(fastChecks);
    }

    public static ScheduledCheckResult empty() {
        return new ScheduledCheckResult(List.of(), List.of());
    }

    public static ScheduledCheckResult fast(RuntimeFastCheck fastCheck) {
        if (fastCheck == null) {
            throw new IllegalArgumentException("Runtime fast check must not be null");
        }
        return new ScheduledCheckResult(List.of(), List.of(), List.of(fastCheck));
    }
}
