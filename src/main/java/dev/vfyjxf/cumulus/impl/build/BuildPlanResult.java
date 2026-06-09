package dev.vfyjxf.cumulus.impl.build;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import java.util.List;

public record BuildPlanResult(
        BuildPlanStatus status,
        List<BuildAction> actions,
        DiagnosticGraph diagnostics) {
    public BuildPlanResult {
        if (status == null) {
            throw new IllegalArgumentException("Build plan status must not be null");
        }
        actions = List.copyOf(actions);
        if (diagnostics == null) {
            throw new IllegalArgumentException("Build plan diagnostics must not be null");
        }
    }
}
