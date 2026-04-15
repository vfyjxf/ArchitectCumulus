package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.request.DataRequest;

import java.util.List;
import java.util.Objects;

public record CheckReport(
        InstanceStatus status,
        List<Diagnostic> diagnostics,
        List<DataRequest> requests,
        InterestUpdate interestUpdate
) implements PassReport {
    public CheckReport {
        Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        requests = List.copyOf(Objects.requireNonNull(requests, "requests"));
        Objects.requireNonNull(interestUpdate, "interestUpdate");
    }

    public static CheckReport ok(InterestUpdate interestUpdate) {
        return new CheckReport(InstanceStatus.OK, List.of(), List.of(), interestUpdate);
    }
}
