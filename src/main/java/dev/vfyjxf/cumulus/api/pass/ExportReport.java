package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.export.ExportView;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.request.DataRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ExportReport(
        InstanceStatus status,
        List<Diagnostic> diagnostics,
        List<DataRequest> requests,
        InterestUpdate interestUpdate,
        Optional<ExportView> export
) implements PassReport {
    public ExportReport {
        Objects.requireNonNull(status, "status");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        requests = List.copyOf(Objects.requireNonNull(requests, "requests"));
        Objects.requireNonNull(interestUpdate, "interestUpdate");
        export = Objects.requireNonNull(export, "export");
    }
}
