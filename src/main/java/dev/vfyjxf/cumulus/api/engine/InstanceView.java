package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.export.ExportView;
import dev.vfyjxf.cumulus.api.interest.InterestView;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

import java.util.List;
import java.util.Optional;

public interface InstanceView {
    InstanceKey key();

    InstanceStatus status();

    List<Diagnostic> diagnostics();

    Optional<RuntimeContextView> runtimeContext();

    Optional<ExportView> exportView();

    InterestView interest();
}
