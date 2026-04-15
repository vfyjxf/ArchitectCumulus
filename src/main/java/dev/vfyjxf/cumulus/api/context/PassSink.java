package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.request.DataRequest;

public interface PassSink {
    void request(DataRequest request);

    void diag(Diagnostic diagnostic);

    void interest(InterestUpdate update);
}
