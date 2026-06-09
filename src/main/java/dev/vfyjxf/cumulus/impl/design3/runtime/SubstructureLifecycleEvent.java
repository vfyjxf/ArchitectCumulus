package dev.vfyjxf.cumulus.impl.design3.runtime;

import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import java.util.Optional;

public record SubstructureLifecycleEvent(
        StructureInstanceKey parent,
        StructureInstanceKey child,
        String occurrenceId,
        Optional<InstanceStatus> previousStatus,
        InstanceStatus currentStatus,
        SubstructureLifecycleTransition transition) {
    public SubstructureLifecycleEvent {
        if (parent == null) {
            throw new IllegalArgumentException("Parent instance key must not be null");
        }
        if (child == null) {
            throw new IllegalArgumentException("Child instance key must not be null");
        }
        if (occurrenceId == null || occurrenceId.isBlank()) {
            throw new IllegalArgumentException("Substructure occurrence id must not be blank");
        }
        if (previousStatus == null) {
            throw new IllegalArgumentException("Previous status must not be null");
        }
        if (currentStatus == null) {
            throw new IllegalArgumentException("Current status must not be null");
        }
        if (transition == null) {
            throw new IllegalArgumentException("Lifecycle transition must not be null");
        }
    }
}
