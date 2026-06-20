package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.target.TargetPattern;

import java.util.LinkedHashSet;
import java.util.Set;

public record StructurePointBuilder(
                String id,
                RelativeBlockPos location,
                TargetPattern target,
                PointRequirement requirement,
                Set<String> groups,
                DiagnosticMetadata diagnostics) {
    public StructurePointBuilder {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Structure point id must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("Structure point location must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Structure point target must not be null");
        }
        if (requirement == null) {
            throw new IllegalArgumentException("Structure point requirement must not be null");
        }
        groups = Set.copyOf(groups);
        if (diagnostics == null) {
            throw new IllegalArgumentException("Structure point diagnostics must not be null");
        }
    }

    public StructurePointBuilder(String id, RelativeBlockPos location, TargetPattern target) {
        this(id, location, target, PointRequirement.required, Set.of(), DiagnosticMetadata.empty());
    }

    public StructurePointBuilder optional() {
        return new StructurePointBuilder(id, location, target, PointRequirement.optional, groups, diagnostics);
    }

    public StructurePointBuilder group(String group) {
        var nextGroups = new LinkedHashSet<>(groups);
        nextGroups.add(group);
        return new StructurePointBuilder(id, location, target, requirement, nextGroups, diagnostics);
    }

    public StructurePointDefinition build() {
        return new StructurePointDefinition(
                        new StructurePointId(id),
                        location,
                        target.target(),
                        target.predicates(),
                        requirement,
                        target.captures(),
                        groups,
                        diagnostics);
    }
}
