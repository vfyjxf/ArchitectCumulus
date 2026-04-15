package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.request.DataRequest;

import java.util.List;

public interface PassReport {
    InstanceStatus status();

    List<Diagnostic> diagnostics();

    List<DataRequest> requests();

    InterestUpdate interestUpdate();
}
