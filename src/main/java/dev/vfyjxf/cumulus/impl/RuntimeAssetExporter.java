package dev.vfyjxf.cumulus.impl;

import dev.vfyjxf.cumulus.api.component.ComponentDescriptor;
import dev.vfyjxf.cumulus.api.datagen.DataDrivenStructureAsset;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuntimeAssetExporter {
    private static final ResourceLocation regionTarget = ResourceLocation.parse("cumulus:region");

    private RuntimeAssetExporter() {}

    public static ExportedRuntimeAsset export(DataDrivenStructureAsset asset, StructureRuntimePlan plan) {
        if (asset == null) {
            throw new IllegalArgumentException("Runtime asset must not be null");
        }
        if (plan == null) {
            throw new IllegalArgumentException("Runtime plan must not be null");
        }

        var values = new LinkedHashMap<String, Object>();
        values.put("id", asset.runtimeAssetId().toString());
        values.put("schema_version", asset.schemaVersion());
        if (asset.definition() != null) {
            values.put("definition", definition(asset.definition()));
        } else {
            values.put("definition", asset.definitionId().toString());
        }
        values.put("debug_asset", asset.debugAsset());
        values.put("runtime_plan", runtimePlan(plan));
        return new ExportedRuntimeAsset(asset.resourcePath(), values, serialize(values));
    }

    private static Map<String, Object> definition(StructureDefinition definition) {
        var values = new LinkedHashMap<String, Object>();
        values.put("id", definition.id().toString());
        values.put("schema_version", definition.schemaVersion());
        values.put("transforms", definition.transforms().stream().map(RuntimeAssetExporter::transform).toList());
        values.put("points", definition.points().stream().map(RuntimeAssetExporter::point).toList());
        values.put("components", definition.components().stream().map(RuntimeAssetExporter::component).toList());
        values.put("versions", definition.versions().stream().map(version -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("version", version.version());
            item.put("definition", version.definitionId().toString());
            return item;
        }).toList());
        values.put("migrations", definition.migrations().stream().map(migration -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("from", migration.fromVersion());
            item.put("to", migration.toVersion());
            item.put("id", migration.migrationId().toString());
            item.put("properties", sortedProperties(migration.properties()));
            return item;
        }).toList());
        return values;
    }

    private static Map<String, Object> point(StructurePointDefinition point) {
        var values = new LinkedHashMap<String, Object>();
        values.put("id", point.id().value());
        values.put("location", point.location().asString());
        values.put("target", point.target().typeId().toString());
        values.put("target_properties", sortedProperties(point.target().properties()));
        values.put("predicates", point.predicates().stream().map(RuntimeAssetExporter::predicate).toList());
        values.put("requirement", point.requirement().name());
        values.put("captures", point.captures().stream().sorted().toList());
        values.put("groups", point.groups().stream().sorted().toList());
        return values;
    }

    private static Map<String, Object> predicate(PredicateDescriptor predicate) {
        var values = new LinkedHashMap<String, Object>();
        values.put("id", predicate.id().toString());
        values.put("properties", sortedProperties(predicate.properties()));
        return values;
    }

    private static Map<String, Object> transform(TransformDescriptor transform) {
        var values = new LinkedHashMap<String, Object>();
        values.put("id", transform.id().toString());
        values.put("properties", sortedProperties(transform.properties()));
        return values;
    }

    private static Map<String, Object> component(ComponentDescriptor component) {
        var values = new LinkedHashMap<String, Object>();
        values.put("name", component.name());
        values.put("type", component.typeId().toString());
        values.put("properties", sortedProperties(component.properties()));
        return values;
    }

    private static Map<String, Object> runtimePlan(StructureRuntimePlan plan) {
        var values = new LinkedHashMap<String, Object>();
        values.put("definition", plan.definitionId().toString());
        values.put("entry_count", plan.entries().size());
        values.put("entries", plan.entries().stream().map(entry -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("id", entry.id());
            item.put("target", entry.targetType().toString());
            item.put("min", entry.min().asString());
            item.put("max", entry.max().asString());
            item.put("properties", sortedProperties(entry.properties()));
            return item;
        }).toList());
        values.put("regions", plan.entries().stream().filter(entry -> regionTarget.equals(entry.targetType())).map(entry -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("id", entry.id());
            item.put("min", entry.min().asString());
            item.put("max", entry.max().asString());
            return item;
        }).toList());
        return values;
    }

    private static Map<String, Object> sortedProperties(Map<String, ?> properties) {
        var sorted = new LinkedHashMap<String, Object>();
        properties.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> sorted.put(entry.getKey(), String.valueOf(entry.getValue())));
        return sorted;
    }

    private static String serialize(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().map(entry -> serialize(entry.getKey().toString()) + ":" + serialize(entry.getValue())).collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        if (value instanceof List<?> list) {
            return list.stream().map(RuntimeAssetExporter::serialize).collect(java.util.stream.Collectors.joining(",", "[", "]"));
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
