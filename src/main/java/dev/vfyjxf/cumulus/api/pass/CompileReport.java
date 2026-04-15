package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CompileReport(
        InstanceStatus status,
        List<Diagnostic> diagnostics,
        List<DataRequest> requests,
        InterestUpdate interestUpdate,
        Optional<RuntimeContextView> compiledContext
) implements PassReport {
    public CompileReport {
        Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        requests = List.copyOf(Objects.requireNonNull(requests, "requests"));
        Objects.requireNonNull(interestUpdate, "interestUpdate");
        compiledContext = Objects.requireNonNull(compiledContext, "compiledContext");
    }

    public static CompileReport ok(RuntimeContextView context, InterestUpdate interestUpdate) {
        return new CompileReport(InstanceStatus.OK, List.of(), List.of(), interestUpdate, Optional.of(context));
    }
}
