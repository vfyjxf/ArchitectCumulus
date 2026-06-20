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

public record BoxComponent(
        String name,
        RelativeBlockPos from,
        RelativeBlockPos to,
        boolean hollow,
        TargetPattern target,
        PointRequirement requirement,
        Set<String> groups,
        DiagnosticMetadata diagnostics) implements StructureComponent {
    public BoxComponent {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Box component name must not be blank");
        }
        if (from == null) {
            throw new IllegalArgumentException("Box component from must not be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Box component to must not be null");
        }
        if (from.x() > to.x() || from.y() > to.y() || from.z() > to.z()) {
            throw new IllegalArgumentException("Box component bounds must be inclusive and non-inverted");
        }
        if (target == null) {
            throw new IllegalArgumentException("Box component target must not be null");
        }
        if (!target.captures().isEmpty()) {
            throw new IllegalArgumentException("Box component target must not define captures");
        }
        if (requirement == null) {
            throw new IllegalArgumentException("Box component requirement must not be null");
        }
        groups = Set.copyOf(groups);
        if (diagnostics == null) {
            throw new IllegalArgumentException("Box component diagnostics must not be null");
        }
    }

    public BoxComponent(String name, RelativeBlockPos from, RelativeBlockPos to) {
        this(name, from, to, false, Targets.block(), PointRequirement.required, Set.of(), DiagnosticMetadata.empty());
    }

    public BoxComponent target(TargetPattern target) {
        return new BoxComponent(name, from, to, hollow, target, requirement, groups, diagnostics);
    }

    public BoxComponent where(PredicateDescriptor predicate) {
        return new BoxComponent(name, from, to, hollow, target.where(predicate), requirement, groups, diagnostics);
    }

    public BoxComponent optional() {
        return new BoxComponent(name, from, to, hollow, target, PointRequirement.optional, groups, diagnostics);
    }

    public BoxComponent group(String group) {
        var nextGroups = new LinkedHashSet<>(groups);
        nextGroups.add(group);
        return new BoxComponent(name, from, to, hollow, target, requirement, nextGroups, diagnostics);
    }

    @Override
    public void emit(ComponentEmitter emitter) {
        for (var x = from.x(); x <= to.x(); x++) {
            for (var y = from.y(); y <= to.y(); y++) {
                for (var z = from.z(); z <= to.z(); z++) {
                    if (hollow && !isShell(x, y, z)) {
                        continue;
                    }
                    emitter.point(new StructurePointDefinition(
                            new StructurePointId(name + "_" + x + "_" + y + "_" + z),
                            RelativeBlockPos.of(x, y, z),
                            target.target(),
                            target.predicates(),
                            requirement,
                            target.captures(),
                            groups,
                            diagnostics));
                }
            }
        }
    }

    private boolean isShell(int x, int y, int z) {
        return x == from.x() || x == to.x()
                || y == from.y() || y == to.y()
                || z == from.z() || z == to.z();
    }
}
