package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.migration.MigrationDescriptor;
import dev.vfyjxf.cumulus.api.migration.StructureVersionDescriptor;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.TargetDescriptor;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import dev.vfyjxf.cumulus.api.component.ComponentDescriptor;
import dev.vfyjxf.cumulus.impl.RuntimePlanEntry;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import net.minecraft.resources.ResourceLocation;

public final class RuntimeAssetLoader {
    public RuntimeAssetLoadResult load(List<RuntimeAssetSource> sources) {
        if (sources == null) {
            throw new IllegalArgumentException("Runtime asset sources must not be null");
        }

        var diagnostics = new ArrayList<DiagnosticNode>();
        var edges = new ArrayList<DiagnosticEdge>();
        var assets = new ArrayList<LoadedRuntimeAsset>();
        var seenPaths = new TreeSet<String>();

        var sortedSources = sources.stream()
                .sorted(Comparator.comparing(RuntimeAssetSource::resourcePath))
                .toList();
        for (var source : sortedSources) {
            if (!seenPaths.add(source.resourcePath())) {
                addDiagnostic(diagnostics, edges, "DUPLICATE_RESOURCE_PATH", source.resourcePath(), source.resourcePath());
                continue;
            }
            var loaded = loadOne(source, diagnostics, edges);
            if (loaded != null) {
                assets.add(loaded);
            }
        }

        return new RuntimeAssetLoadResult(
                assets,
                new DiagnosticGraph("root", List.copyOf(diagnostics), List.copyOf(edges)));
    }

    private static LoadedRuntimeAsset loadOne(
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var values = source.values();
        var valid = true;

        var id = string(values, "id", source, diagnostics, edges);
        var schemaVersion = integer(values, "schema_version", source, diagnostics, edges);
        var definition = definition(values.get("definition"), source, diagnostics, edges);
        var debugAsset = bool(values, "debug_asset", source, diagnostics, edges);
        var runtimePlanValues = map(values, "runtime_plan", source, diagnostics, edges);
        valid &= id != null && schemaVersion != null && definition.definitionId() != null && debugAsset != null && runtimePlanValues != null;

        var predicates = loadPredicates(values.get("predicates"), source, diagnostics, edges);
        if (predicates == null) {
            valid = false;
        }

        var plan = runtimePlanValues == null ? null : runtimePlan(runtimePlanValues, source, diagnostics, edges);
        if (plan == null) {
            valid = false;
        }
        if (plan != null && definition.definitionId() != null && !plan.definitionId().equals(definition.definitionId())) {
            addDiagnostic(diagnostics, edges, "MISMATCHED_DEFINITION", source.resourcePath(), source.resourcePath());
            valid = false;
        }

        if (!valid) {
            return null;
        }
        return new LoadedRuntimeAsset(
                source.resourcePath(),
                ResourceLocation.parse(id),
                schemaVersion,
                definition.definitionId(),
                definition.definition(),
                debugAsset,
                plan,
                predicates);
    }

    private static LoadedDefinition definition(
            Object value,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        if (value instanceof String text && !text.isBlank()) {
            return new LoadedDefinition(ResourceLocation.parse(text), Optional.empty());
        }
        if (value instanceof Map<?, ?> raw) {
            var values = toObjectMap(raw);
            var definition = loadDefinition(values, source, diagnostics, edges);
            if (definition == null) {
                return new LoadedDefinition(null, Optional.empty());
            }
            return new LoadedDefinition(definition.id(), Optional.of(definition));
        }
        addDiagnostic(diagnostics, edges, "MISSING_DEFINITION", source.resourcePath(), source.resourcePath());
        return new LoadedDefinition(null, Optional.empty());
    }

    private static StructureDefinition loadDefinition(
            Map<String, Object> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var id = string(values, "id", source, diagnostics, edges);
        var schemaVersion = integer(values, "schema_version", source, diagnostics, edges);
        var transformsValues = list(values, "transforms", source, diagnostics, edges);
        var pointsValues = list(values, "points", source, diagnostics, edges);
        var componentsValues = list(values, "components", source, diagnostics, edges);
        var versionsValues = list(values, "versions", source, diagnostics, edges);
        var migrationsValues = list(values, "migrations", source, diagnostics, edges);
        if (id == null
                || schemaVersion == null
                || transformsValues == null
                || pointsValues == null
                || componentsValues == null
                || versionsValues == null
                || migrationsValues == null) {
            return null;
        }

        var transforms = transforms(transformsValues, source, diagnostics, edges);
        var points = points(pointsValues, source, diagnostics, edges);
        var components = components(componentsValues, source, diagnostics, edges);
        var versions = versions(versionsValues, source, diagnostics, edges);
        var migrations = migrations(migrationsValues, source, diagnostics, edges);
        if (transforms == null || points == null || components == null || versions == null || migrations == null) {
            return null;
        }
        return new StructureDefinition(
                ResourceLocation.parse(id),
                schemaVersion,
                transforms,
                points,
                components,
                List.of(),
                versions,
                migrations,
                DiagnosticMetadata.empty());
    }

    private static List<TransformDescriptor> transforms(
            List<?> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var transforms = new ArrayList<TransformDescriptor>();
        for (var item : values) {
            if (!(item instanceof Map<?, ?> raw)) {
                addDiagnostic(diagnostics, edges, "INVALID_TRANSFORM", source.resourcePath(), source.resourcePath());
                return null;
            }
            var valuesMap = toObjectMap(raw);
            var id = string(valuesMap, "id", source, diagnostics, edges);
            var properties = stringMap(valuesMap.get("properties"), source, diagnostics, edges, "INVALID_TRANSFORM");
            if (id == null || properties == null) {
                return null;
            }
            transforms.add(new TransformDescriptor(ResourceLocation.parse(id), properties));
        }
        return transforms;
    }

    private static List<StructurePointDefinition> points(
            List<?> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var points = new ArrayList<StructurePointDefinition>();
        for (var item : values) {
            if (!(item instanceof Map<?, ?> raw)) {
                addDiagnostic(diagnostics, edges, "INVALID_POINT", source.resourcePath(), source.resourcePath());
                return null;
            }
            var point = point(toObjectMap(raw), source, diagnostics, edges);
            if (point == null) {
                return null;
            }
            points.add(point);
        }
        return points;
    }

    private static StructurePointDefinition point(
            Map<String, Object> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var id = string(values, "id", source, diagnostics, edges);
        var location = string(values, "location", source, diagnostics, edges);
        var target = string(values, "target", source, diagnostics, edges);
        var targetProperties = stringMap(values.get("target_properties"), source, diagnostics, edges, "INVALID_POINT");
        var predicateValues = list(values, "predicates", source, diagnostics, edges);
        var requirement = string(values, "requirement", source, diagnostics, edges);
        var captureValues = list(values, "captures", source, diagnostics, edges);
        var groupValues = list(values, "groups", source, diagnostics, edges);
        if (id == null
                || location == null
                || target == null
                || targetProperties == null
                || predicateValues == null
                || requirement == null
                || captureValues == null
                || groupValues == null) {
            return null;
        }
        var predicates = predicates(predicateValues, source, diagnostics, edges);
        var captures = stringSet(captureValues, source, diagnostics, edges, "INVALID_POINT");
        var groups = stringSet(groupValues, source, diagnostics, edges, "INVALID_POINT");
        if (predicates == null || captures == null || groups == null) {
            return null;
        }
        try {
            return new StructurePointDefinition(
                    new StructurePointId(id),
                    parsePos(location),
                    new TargetDescriptor(ResourceLocation.parse(target), targetProperties),
                    predicates,
                    PointRequirement.valueOf(requirement),
                    captures,
                    groups,
                    DiagnosticMetadata.empty());
        } catch (IllegalArgumentException ex) {
            addDiagnostic(diagnostics, edges, "INVALID_POINT", source.resourcePath(), source.resourcePath());
            return null;
        }
    }

    private static List<PredicateDescriptor> predicates(
            List<?> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var predicates = new ArrayList<PredicateDescriptor>();
        for (var item : values) {
            if (!(item instanceof Map<?, ?> raw)) {
                addDiagnostic(diagnostics, edges, "INVALID_PREDICATE", source.resourcePath(), source.resourcePath());
                return null;
            }
            var predicateMap = toObjectMap(raw);
            var id = string(predicateMap, "id", source, diagnostics, edges);
            var properties = stringMap(predicateMap.get("properties"), source, diagnostics, edges, "INVALID_PREDICATE");
            if (id == null || properties == null) {
                return null;
            }
            predicates.add(new PredicateDescriptor(ResourceLocation.parse(id), properties));
        }
        return predicates;
    }

    private static List<ComponentDescriptor> components(
            List<?> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var components = new ArrayList<ComponentDescriptor>();
        for (var item : values) {
            if (!(item instanceof Map<?, ?> raw)) {
                addDiagnostic(diagnostics, edges, "INVALID_COMPONENT", source.resourcePath(), source.resourcePath());
                return null;
            }
            var component = toObjectMap(raw);
            var name = string(component, "name", source, diagnostics, edges);
            var type = string(component, "type", source, diagnostics, edges);
            var properties = objectMap(component.get("properties"), source, diagnostics, edges, "INVALID_COMPONENT");
            if (name == null || type == null || properties == null) {
                return null;
            }
            components.add(new ComponentDescriptor(name, ResourceLocation.parse(type), properties));
        }
        return components;
    }

    private static List<StructureVersionDescriptor> versions(
            List<?> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var versions = new ArrayList<StructureVersionDescriptor>();
        for (var item : values) {
            if (!(item instanceof Map<?, ?> raw)) {
                addDiagnostic(diagnostics, edges, "INVALID_VERSION", source.resourcePath(), source.resourcePath());
                return null;
            }
            var version = toObjectMap(raw);
            var number = integer(version, "version", source, diagnostics, edges);
            var definition = string(version, "definition", source, diagnostics, edges);
            if (number == null || definition == null) {
                return null;
            }
            versions.add(new StructureVersionDescriptor(number, ResourceLocation.parse(definition)));
        }
        return versions;
    }

    private static List<MigrationDescriptor> migrations(
            List<?> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var migrations = new ArrayList<MigrationDescriptor>();
        for (var item : values) {
            if (!(item instanceof Map<?, ?> raw)) {
                addDiagnostic(diagnostics, edges, "INVALID_MIGRATION", source.resourcePath(), source.resourcePath());
                return null;
            }
            var migration = toObjectMap(raw);
            var from = integer(migration, "from", source, diagnostics, edges);
            var to = integer(migration, "to", source, diagnostics, edges);
            var id = string(migration, "id", source, diagnostics, edges);
            var properties = stringMap(migration.get("properties"), source, diagnostics, edges, "INVALID_MIGRATION");
            if (from == null || to == null || id == null || properties == null) {
                return null;
            }
            migrations.add(new MigrationDescriptor(from, to, ResourceLocation.parse(id), properties));
        }
        return migrations;
    }

    private static StructureRuntimePlan runtimePlan(
            Map<String, Object> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var definition = string(values, "definition", source, diagnostics, edges);
        var entriesValues = list(values, "entries", source, diagnostics, edges);
        if (definition == null || entriesValues == null) {
            return null;
        }
        var entries = new ArrayList<RuntimePlanEntry>();
        for (var item : entriesValues) {
            if (!(item instanceof Map<?, ?> rawEntry)) {
                addDiagnostic(diagnostics, edges, "INVALID_RUNTIME_ENTRY", source.resourcePath(), source.resourcePath());
                return null;
            }
            var entry = toStringMap(rawEntry);
            var id = entry.get("id");
            var target = entry.get("target");
            var min = entry.get("min");
            var max = entry.get("max");
            if (id == null || target == null || min == null || max == null) {
                addDiagnostic(diagnostics, edges, "INVALID_RUNTIME_ENTRY", source.resourcePath(), source.resourcePath());
                return null;
            }
            var properties = stringMapOrEmpty(rawEntry.get("properties"));
            entries.add(new RuntimePlanEntry(
                    id,
                    ResourceLocation.parse(target),
                    parsePos(min),
                    parsePos(max),
                    properties));
        }
        return new StructureRuntimePlan(
                ResourceLocation.parse(definition),
                entries,
                List.of(),
                List.of(),
                new VisualizationModel(definition + "/loaded", List.of()));
    }

    private static List<PredicateDescriptor> loadPredicates(
            Object value,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            addDiagnostic(diagnostics, edges, "INVALID_PREDICATE", source.resourcePath(), source.resourcePath());
            return null;
        }
        var predicates = new ArrayList<PredicateDescriptor>();
        for (var item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                addDiagnostic(diagnostics, edges, "INVALID_PREDICATE", source.resourcePath(), source.resourcePath());
                return null;
            }
            try {
                predicates.add(PredicateDescriptors.fromSerialized(toStringMap(map)));
            } catch (IllegalArgumentException ex) {
                addDiagnostic(diagnostics, edges, "INVALID_PREDICATE", source.resourcePath(), source.resourcePath());
                return null;
            }
        }
        return predicates;
    }

    private static Map<String, String> toStringMap(Map<?, ?> raw) {
        var values = new LinkedHashMap<String, String>();
        raw.entrySet().stream()
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .forEach(entry -> values.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())));
        return values;
    }

    private static Map<String, Object> toObjectMap(Map<?, ?> raw) {
        var values = new LinkedHashMap<String, Object>();
        raw.entrySet().stream()
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .forEach(entry -> values.put(String.valueOf(entry.getKey()), entry.getValue()));
        return values;
    }

    private static Map<String, String> stringMap(
            Object value,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges,
            String diagnosticCode) {
        if (!(value instanceof Map<?, ?> raw)) {
            addDiagnostic(diagnostics, edges, diagnosticCode, source.resourcePath(), source.resourcePath());
            return null;
        }
        var values = new LinkedHashMap<String, String>();
        raw.entrySet().stream()
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .forEach(entry -> values.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())));
        return values;
    }

    private static Map<String, String> stringMapOrEmpty(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        var values = new LinkedHashMap<String, String>();
        raw.entrySet().stream()
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .forEach(entry -> values.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())));
        return values;
    }

    private static Map<String, Object> objectMap(
            Object value,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges,
            String diagnosticCode) {
        if (!(value instanceof Map<?, ?> raw)) {
            addDiagnostic(diagnostics, edges, diagnosticCode, source.resourcePath(), source.resourcePath());
            return null;
        }
        return toObjectMap(raw);
    }

    private static LinkedHashSet<String> stringSet(
            List<?> values,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges,
            String diagnosticCode) {
        var result = new LinkedHashSet<String>();
        for (var value : values) {
            if (!(value instanceof String text)) {
                addDiagnostic(diagnostics, edges, diagnosticCode, source.resourcePath(), source.resourcePath());
                return null;
            }
            result.add(text);
        }
        return result;
    }

    private static String string(
            Map<String, Object> values,
            String key,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var value = values.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        addDiagnostic(diagnostics, edges, "MISSING_" + key.toUpperCase(), source.resourcePath(), source.resourcePath());
        return null;
    }

    private static Integer integer(
            Map<String, Object> values,
            String key,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var value = values.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        addDiagnostic(diagnostics, edges, "MISSING_" + key.toUpperCase(), source.resourcePath(), source.resourcePath());
        return null;
    }

    private static Boolean bool(
            Map<String, Object> values,
            String key,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var value = values.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        addDiagnostic(diagnostics, edges, "MISSING_" + key.toUpperCase(), source.resourcePath(), source.resourcePath());
        return null;
    }

    private static Map<String, Object> map(
            Map<String, Object> values,
            String key,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var value = values.get(key);
        if (value instanceof Map<?, ?> raw) {
            var map = new LinkedHashMap<String, Object>();
            raw.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .forEach(entry -> map.put(String.valueOf(entry.getKey()), entry.getValue()));
            return map;
        }
        addDiagnostic(diagnostics, edges, "MISSING_" + key.toUpperCase(), source.resourcePath(), source.resourcePath());
        return null;
    }

    private static List<?> list(
            Map<String, Object> values,
            String key,
            RuntimeAssetSource source,
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges) {
        var value = values.get(key);
        if (value instanceof List<?> list) {
            return list;
        }
        addDiagnostic(diagnostics, edges, "MISSING_" + key.toUpperCase(), source.resourcePath(), source.resourcePath());
        return null;
    }

    private static RelativeBlockPos parsePos(String value) {
        var parts = value.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Relative position must have x,y,z parts");
        }
        return RelativeBlockPos.of(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
    }

    private static void addDiagnostic(
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges,
            String code,
            String message,
            String sourcePath) {
        if (diagnostics.isEmpty()) {
            diagnostics.add(new DiagnosticNode(
                    "root",
                    Severity.INFO,
                    "Runtime asset loading failed",
                    "diagnostic.cumulus.asset_load_failed",
                    List.of(),
                    "runtime/assets",
                    List.of()));
        }
        diagnostics.add(new DiagnosticNode(
                code,
                Severity.ERROR,
                message,
                "diagnostic.cumulus." + code.toLowerCase(),
                List.of(),
                sourcePath,
                List.of()));
        edges.add(new DiagnosticEdge("root", code, "caused_by"));
    }

    private record LoadedDefinition(
            ResourceLocation definitionId,
            Optional<StructureDefinition> definition) {
    }
}
