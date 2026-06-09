package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.predicate.PredicateRegistry;
import dev.vfyjxf.cumulus.api.predicate.PredicateTarget;

public final class PredicateEvaluator {
    private PredicateEvaluator() {
    }

    public static Evaluation evaluate(PredicateDescriptor predicate, SnapshotTarget target) {
        return evaluate(predicate, target, PredicateRegistry.defaults());
    }

    public static Evaluation evaluate(PredicateDescriptor predicate, SnapshotTarget target, PredicateRegistry registry) {
        if (predicate == null) {
            throw new IllegalArgumentException("Predicate must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target must not be null");
        }
        if (registry == null) {
            throw new IllegalArgumentException("Predicate registry must not be null");
        }
        var result = registry.evaluate(predicate, new PredicateTarget(target.value(), target.properties()));
        if (result.isEmpty()) {
            return Evaluation.UNSUPPORTED;
        }
        return result.orElseThrow()
                ? Evaluation.MATCH
                : Evaluation.FAILED;
    }

    public static boolean matches(PredicateDescriptor predicate, SnapshotTarget target) {
        return evaluate(predicate, target) == Evaluation.MATCH;
    }

    public enum Evaluation {
        MATCH,
        FAILED,
        UNSUPPORTED
    }
}
