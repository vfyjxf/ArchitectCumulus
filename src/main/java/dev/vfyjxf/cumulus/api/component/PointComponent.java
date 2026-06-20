package dev.vfyjxf.cumulus.api.component;

import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.target.TargetPattern;
import dev.vfyjxf.cumulus.api.target.Targets;

import java.util.LinkedHashSet;
import java.util.Set;

public record PointComponent(
                String name,
                RelativeBlockPos location,
                TargetPattern target,
                PointRequirement requirement,
                Set<String> groups,
                DiagnosticMetadata diagnostics) implements StructureComponent {
    public PointComponent {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Point component name must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("Point component location must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Point component target must not be null");
        }
        if (requirement == null) {
            throw new IllegalArgumentException("Point component requirement must not be null");
        }
        groups = Set.copyOf(groups);
        if (diagnostics == null) {
            throw new IllegalArgumentException("Point component diagnostics must not be null");
        }
    }

    public PointComponent(String name, RelativeBlockPos location) {
        this(name, location, Targets.block(), PointRequirement.required, Set.of(), DiagnosticMetadata.empty());
    }

    public PointComponent target(TargetPattern target) {
        return new PointComponent(name, location, target, requirement, groups, diagnostics);
    }

    public PointComponent where(PredicateDescriptor predicate) {
        return new PointComponent(name, location, target.where(predicate), requirement, groups, diagnostics);
    }

    public PointComponent optional() {
        return new PointComponent(name, location, target, PointRequirement.optional, groups, diagnostics);
    }

    public PointComponent group(String group) {
        var nextGroups = new LinkedHashSet<>(groups);
        nextGroups.add(group);
        return new PointComponent(name, location, target, requirement, nextGroups, diagnostics);
    }

    @Override
    public void emit(ComponentEmitter emitter) {
        emitter.point(new StructurePointDefinition(
                        new StructurePointId(name),
                        location,
                        target.target(),
                        target.predicates(),
                        requirement,
                        target.captures(),
                        groups,
                        diagnostics));
    }
}
