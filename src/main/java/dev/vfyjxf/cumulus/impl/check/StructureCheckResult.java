package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;

import java.util.List;

public record StructureCheckResult(
                InstanceStatus status,
                RuntimeStructureContext context,
                DiagnosticGraph diagnostics,
                VisualizationModel visualization,
                List<DataRequest> requests) {
    public StructureCheckResult {
        if (status == null) {
            throw new IllegalArgumentException("Check status must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Check context must not be null");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("Check diagnostics must not be null");
        }
        if (visualization == null) {
            throw new IllegalArgumentException("Check visualization must not be null");
        }
        requests = List.copyOf(requests);
    }
}
