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

public record RegionComponent(
        String name,
        RelativeBlockPos from,
        RelativeBlockPos to,
        TargetPattern target,
        PointRequirement requirement,
        Set<String> groups,
        DiagnosticMetadata diagnostics) implements StructureComponent {
    public RegionComponent {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Region component name must not be blank");
        }
        Targets.validateRegionBounds(from, to);
        if (target == null) {
            throw new IllegalArgumentException("Region component target must not be null");
        }
        if (requirement == null) {
            throw new IllegalArgumentException("Region component requirement must not be null");
        }
        groups = Set.copyOf(groups);
        if (diagnostics == null) {
            throw new IllegalArgumentException("Region component diagnostics must not be null");
        }
    }

    public RegionComponent target(TargetPattern target) {
        return new RegionComponent(name, from, to, target, requirement, groups, diagnostics);
    }

    public RegionComponent where(PredicateDescriptor predicate) {
        return new RegionComponent(name, from, to, target.where(predicate), requirement, groups, diagnostics);
    }

    public RegionComponent optional() {
        return new RegionComponent(name, from, to, target, PointRequirement.optional, groups, diagnostics);
    }

    public RegionComponent group(String group) {
        var nextGroups = new LinkedHashSet<>(groups);
        nextGroups.add(group);
        return new RegionComponent(name, from, to, target, requirement, nextGroups, diagnostics);
    }

    @Override
    public void emit(ComponentEmitter emitter) {
        emitter.point(new StructurePointDefinition(
                new StructurePointId(name),
                from,
                target.target(),
                target.predicates(),
                requirement,
                target.captures(),
                groups,
                diagnostics));
    }
}
