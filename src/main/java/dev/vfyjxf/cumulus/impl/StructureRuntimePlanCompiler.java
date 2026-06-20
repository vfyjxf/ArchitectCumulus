package dev.vfyjxf.cumulus.impl;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.diagnostic.FixHint;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import dev.vfyjxf.cumulus.api.visualization.VisualizationElement;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;
import dev.vfyjxf.cumulus.impl.check.CheckPlanCandidate;
import dev.vfyjxf.cumulus.impl.check.CheckPlanEntry;
import dev.vfyjxf.cumulus.impl.check.StructureCheckPlan;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StructureRuntimePlanCompiler {
    private static final ResourceLocation regionTarget = ResourceLocation.parse("cumulus:region");
    private static final ResourceLocation substructureComponent = ResourceLocation.parse("cumulus:substructure");
    private static final ResourceLocation rotateY90 = ResourceLocation.parse("cumulus:rotate_y_90");
    private static final ResourceLocation rotateY180 = ResourceLocation.parse("cumulus:rotate_y_180");
    private static final ResourceLocation rotateY270 = ResourceLocation.parse("cumulus:rotate_y_270");
    private static final ResourceLocation horizontalMirror = ResourceLocation.parse("cumulus:horizontal_mirror");

    private StructureRuntimePlanCompiler() {}

    public static StructureRuntimePlan compile(StructureDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }

        var entries = new ArrayList<RuntimePlanEntry>();
        var buildHints = new ArrayList<FixHint>();
        var visualizationElements = new ArrayList<VisualizationElement>();

        for (var point : definition.points()) {
            var min = point.location();
            var max = point.location();
            if (regionTarget.equals(point.target().typeId())) {
                min = parsePos(point.target().properties().get("from"));
                max = parsePos(point.target().properties().get("to"));
            }
            entries.add(new RuntimePlanEntry(
                            point.id().value(),
                            point.target().typeId(),
                            min,
                            max,
                            point.target().properties()));
            buildHints.add(new FixHint("place_or_validate", point.id().value(), point.id().value(), Map.of("target", point.target().typeId().toString(), "from", min.asString(), "to", max.asString())));
            visualizationElements.add(VisualizationElement.region(point.id().value() + "-overlay", point.target().typeId().getPath(), min, max, Severity.info, point.groups().isEmpty() ? "structure" : point.groups().iterator().next(), "visualization.cumulus." + point.id().value(), 0));
        }

        var substructures = definition.components().stream().filter(component -> component.typeId().equals(substructureComponent)).toList();
        return new StructureRuntimePlan(
                        definition.id(),
                        entries,
                        substructures,
                        buildHints,
                        new VisualizationModel(definition.id().toString(), visualizationElements));
    }

    public static StructureCheckPlan compileCheckPlan(StructureDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        var candidates = new ArrayList<CheckPlanCandidate>();
        for (var transform : definition.transforms()) {
            var entries = new ArrayList<CheckPlanEntry>();
            for (var point : definition.points()) {
                var bounds = bounds(point, transform);
                entries.add(new CheckPlanEntry(point, bounds.min(), bounds.max()));
            }
            candidates.add(new CheckPlanCandidate(transform, entries));
        }
        return new StructureCheckPlan(definition.id(), definition, candidates);
    }

    public static RelativeBlockPos transform(RelativeBlockPos position, TransformDescriptor transform) {
        if (position == null) {
            throw new IllegalArgumentException("Relative position must not be null");
        }
        if (transform == null) {
            throw new IllegalArgumentException("Transform must not be null");
        }
        var id = transform.id();
        if (id.equals(rotateY90)) {
            return RelativeBlockPos.of(-position.z(), position.y(), position.x());
        }
        if (id.equals(rotateY180)) {
            return RelativeBlockPos.of(-position.x(), position.y(), -position.z());
        }
        if (id.equals(rotateY270)) {
            return RelativeBlockPos.of(position.z(), position.y(), -position.x());
        }
        if (id.equals(horizontalMirror)) {
            return RelativeBlockPos.of(-position.x(), position.y(), position.z());
        }
        return position;
    }

    private static RelativeBlockPos parsePos(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Relative position string must not be blank");
        }
        var parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Relative position must have x,y,z parts");
        }
        return RelativeBlockPos.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    private static Bounds bounds(StructurePointDefinition point, TransformDescriptor transform) {
        if (regionTarget.equals(point.target().typeId())) {
            return transformedBounds(parsePos(point.target().properties().get("from")), parsePos(point.target().properties().get("to")), transform);
        }
        return transformedBounds(point.location(), point.location(), transform);
    }

    private static Bounds transformedBounds(RelativeBlockPos from, RelativeBlockPos to, TransformDescriptor transformDescriptor) {
        var corners = List.of(RelativeBlockPos.of(from.x(), from.y(), from.z()), RelativeBlockPos.of(from.x(), from.y(), to.z()), RelativeBlockPos.of(from.x(), to.y(), from.z()), RelativeBlockPos.of(from.x(), to.y(), to.z()), RelativeBlockPos.of(to.x(), from.y(), from.z()), RelativeBlockPos.of(to.x(), from.y(), to.z()), RelativeBlockPos.of(to.x(), to.y(), from.z()), RelativeBlockPos.of(to.x(), to.y(), to.z()));
        var transformed = corners.stream().map(position -> transform(position, transformDescriptor)).toList();
        var minX = transformed.stream().mapToInt(RelativeBlockPos::x).min().orElseThrow();
        var minY = transformed.stream().mapToInt(RelativeBlockPos::y).min().orElseThrow();
        var minZ = transformed.stream().mapToInt(RelativeBlockPos::z).min().orElseThrow();
        var maxX = transformed.stream().mapToInt(RelativeBlockPos::x).max().orElseThrow();
        var maxY = transformed.stream().mapToInt(RelativeBlockPos::y).max().orElseThrow();
        var maxZ = transformed.stream().mapToInt(RelativeBlockPos::z).max().orElseThrow();
        return new Bounds(
                        RelativeBlockPos.of(minX, minY, minZ),
                        RelativeBlockPos.of(maxX, maxY, maxZ));
    }

    private record Bounds(RelativeBlockPos min, RelativeBlockPos max) {
    }
}
