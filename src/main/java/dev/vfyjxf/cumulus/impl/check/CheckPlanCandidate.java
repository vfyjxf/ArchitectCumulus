package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import java.util.List;

public record CheckPlanCandidate(
        TransformDescriptor transform,
        List<CheckPlanEntry> entries) {
    public CheckPlanCandidate {
        if (transform == null) {
            throw new IllegalArgumentException("Check plan transform must not be null");
        }
        entries = List.copyOf(entries);
    }
}
