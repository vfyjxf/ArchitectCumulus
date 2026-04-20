package dev.vfyjxf.cumulus.api.target;

import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record TargetPattern(
        TargetDescriptor target,
        List<PredicateDescriptor> predicates,
        Set<String> captures) {
    public TargetPattern {
        predicates = List.copyOf(predicates);
        captures = Set.copyOf(captures);
    }

    public TargetPattern where(PredicateDescriptor predicate) {
        var next = new ArrayList<>(predicates);
        next.add(predicate);
        return new TargetPattern(target, next, captures);
    }

    public TargetPattern capture(String captureName) {
        var next = new LinkedHashSet<>(captures);
        next.add(captureName);
        return new TargetPattern(target, predicates, next);
    }
}
