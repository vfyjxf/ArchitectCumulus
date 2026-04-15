package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.binding.BindingsView;
import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.request.DataRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record BindReport(
        InstanceStatus status,
        List<Diagnostic> diagnostics,
        List<DataRequest> requests,
        InterestUpdate interestUpdate,
        Optional<BindingsView> bindings
) implements PassReport {
    public BindReport {
        Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        requests = List.copyOf(Objects.requireNonNull(requests, "requests"));
        Objects.requireNonNull(interestUpdate, "interestUpdate");
        bindings = Objects.requireNonNull(bindings, "bindings");
    }

    public static BindReport ok(BindingsView bindings, InterestUpdate interestUpdate) {
        return new BindReport(InstanceStatus.OK, List.of(), List.of(), interestUpdate, Optional.of(bindings));
    }

    public static BindReport incomplete(List<DataRequest> requests, List<Diagnostic> diagnostics, InterestUpdate interestUpdate) {
        return new BindReport(InstanceStatus.INCOMPLETE, diagnostics, requests, interestUpdate, Optional.empty());
    }

    public static BindReport error(List<Diagnostic> diagnostics, InterestUpdate interestUpdate) {
        return new BindReport(InstanceStatus.ERROR, diagnostics, List.of(), interestUpdate, Optional.empty());
    }
}
