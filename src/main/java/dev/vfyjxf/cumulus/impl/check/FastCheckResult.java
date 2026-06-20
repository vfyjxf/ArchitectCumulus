package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;

public record FastCheckResult(
                InstanceStatus status,
                int candidatesEvaluated,
                int entriesEvaluated,
                int validCandidateIndex) {
    public FastCheckResult {
        if (status == null) {
            throw new IllegalArgumentException("Fast check status must not be null");
        }
        if (candidatesEvaluated < 0) {
            throw new IllegalArgumentException("Candidates evaluated must not be negative");
        }
        if (entriesEvaluated < 0) {
            throw new IllegalArgumentException("Entries evaluated must not be negative");
        }
        if (validCandidateIndex < -1) {
            throw new IllegalArgumentException("Valid candidate index must be -1 or non-negative");
        }
    }
}
