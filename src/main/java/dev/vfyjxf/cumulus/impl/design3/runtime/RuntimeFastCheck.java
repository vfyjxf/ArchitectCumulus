package dev.vfyjxf.cumulus.impl.design3.runtime;

import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.impl.design3.check.FastCheckResult;

public record RuntimeFastCheck(
        StructureInstanceKey instance,
        FastCheckResult result) {
    public RuntimeFastCheck {
        if (instance == null) {
            throw new IllegalArgumentException("Runtime fast-check instance must not be null");
        }
        if (result == null) {
            throw new IllegalArgumentException("Runtime fast-check result must not be null");
        }
    }

    public dev.vfyjxf.cumulus.api.runtime.InstanceStatus status() {
        return result.status();
    }

    public int candidatesEvaluated() {
        return result.candidatesEvaluated();
    }

    public int entriesEvaluated() {
        return result.entriesEvaluated();
    }

    public int validCandidateIndex() {
        return result.validCandidateIndex();
    }
}
