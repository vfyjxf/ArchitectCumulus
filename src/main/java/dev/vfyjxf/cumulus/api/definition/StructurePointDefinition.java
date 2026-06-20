package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.target.TargetDescriptor;

import java.util.List;
import java.util.Set;

public record StructurePointDefinition(
                StructurePointId id,
                RelativeBlockPos location,
                TargetDescriptor target,
                List<PredicateDescriptor> predicates,
                PointRequirement requirement,
                Set<String> captures,
                Set<String> groups,
                DiagnosticMetadata diagnostics) {
    public StructurePointDefinition {
        if (id == null)
            throw new IllegalArgumentException("Point id must not be null");
        if (location == null)
            throw new IllegalArgumentException("Point location must not be null");
        if (target == null)
            throw new IllegalArgumentException("Point target must not be null");
        if (requirement == null)
            throw new IllegalArgumentException("Point requirement must not be null");
        predicates = List.copyOf(predicates);
        captures = Set.copyOf(captures);
        groups = Set.copyOf(groups);
        if (diagnostics == null)
            throw new IllegalArgumentException("Point diagnostics must not be null");
    }
}
