package dev.vfyjxf.cumulus.impl.design3;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.datagen.DataDrivenStructureAsset;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.migration.MigrationDescriptor;
import dev.vfyjxf.cumulus.api.migration.StructureVersionDescriptor;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeAssetExporterTest {
    @Test
    void exportsDeterministicJsonLikeRuntimeAssetContent() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .transforms(
                        Transforms.horizontalFacing(),
                        Transforms.custom(ResourceLocation.parse("cumulus:offset"), Map.of(
                                "z", "-1",
                                "x", "2")))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .component(Components.region("door", RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 2, 0)))
                .component(Components.substructure(
                        "leaf",
                        ResourceLocation.parse("cumulus:door_leaf"),
                        RelativeBlockPos.of(0, 1, 0)))
                .version(new StructureVersionDescriptor(1, ResourceLocation.parse("cumulus:machine_v1")))
                .version(new StructureVersionDescriptor(2, ResourceLocation.parse("cumulus:machine")))
                .migration(new MigrationDescriptor(
                        1,
                        2,
                        ResourceLocation.parse("cumulus:machine_upgrade"),
                        Map.of(
                                "strategy", "replace",
                                "preserve_context", "true")))
                .build();
        var plan = StructureRuntimePlanCompiler.compile(definition);
        var asset = DataDrivenStructureAsset.fromDefinition(
                ResourceLocation.parse("cumulus:machines/machine"),
                2,
                definition,
                true);

        var exported = RuntimeAssetExporter.export(asset, plan);

        assertEquals("data/cumulus/cumulus/structures/machines/machine.json", exported.resourcePath());
        assertEquals(orderedMap(
                "id", "cumulus:machines/machine",
                "schema_version", 2,
                "definition", orderedMap(
                        "id", "cumulus:machine",
                        "schema_version", 1,
                        "transforms", java.util.List.of(
                                orderedMap(
                                        "id", "cumulus:horizontal_facing",
                                        "properties", orderedMap()),
                                orderedMap(
                                        "id", "cumulus:offset",
                                        "properties", orderedMap(
                                                "x", "2",
                                                "z", "-1"))),
                        "points", java.util.List.of(
                                orderedMap(
                                        "id", "controller",
                                        "location", "0,0,0",
                                        "target", "cumulus:block",
                                        "target_properties", orderedMap(),
                                        "predicates", java.util.List.of(),
                                        "requirement", "REQUIRED",
                                        "captures", java.util.List.of(),
                                        "groups", java.util.List.of()),
                                orderedMap(
                                        "id", "door",
                                        "location", "-1,0,0",
                                        "target", "cumulus:region",
                                        "target_properties", orderedMap(
                                                "from", "-1,0,0",
                                                "to", "1,2,0"),
                                        "predicates", java.util.List.of(),
                                        "requirement", "REQUIRED",
                                        "captures", java.util.List.of(),
                                        "groups", java.util.List.of())),
                        "components", java.util.List.of(orderedMap(
                                "name", "leaf",
                                "type", "cumulus:substructure",
                                "properties", orderedMap(
                                        "anchor_offset", "0,1,0",
                                        "structure", "cumulus:door_leaf"))),
                        "versions", java.util.List.of(
                                orderedMap(
                                        "version", 1,
                                        "definition", "cumulus:machine_v1"),
                                orderedMap(
                                        "version", 2,
                                        "definition", "cumulus:machine")),
                        "migrations", java.util.List.of(orderedMap(
                                "from", 1,
                                "to", 2,
                                "id", "cumulus:machine_upgrade",
                                "properties", orderedMap(
                                        "preserve_context", "true",
                                        "strategy", "replace")))),
                "debug_asset", true,
                "runtime_plan", orderedMap(
                        "definition", "cumulus:machine",
                        "entry_count", 2,
                        "entries", java.util.List.of(
                                orderedMap(
                                        "id", "controller",
                                        "target", "cumulus:block",
                                        "min", "0,0,0",
                                        "max", "0,0,0",
                                        "properties", orderedMap()),
                                orderedMap(
                                        "id", "door",
                                        "target", "cumulus:region",
                                        "min", "-1,0,0",
                                        "max", "1,2,0",
                                        "properties", orderedMap(
                                                "from", "-1,0,0",
                                                "to", "1,2,0"))),
                        "regions", java.util.List.of(orderedMap(
                                "id", "door",
                                "min", "-1,0,0",
                                "max", "1,2,0")))),
                exported.values());
        assertEquals(
                "{\"id\":\"cumulus:machines/machine\",\"schema_version\":2,\"definition\":{\"id\":\"cumulus:machine\",\"schema_version\":1,\"transforms\":[{\"id\":\"cumulus:horizontal_facing\",\"properties\":{}},{\"id\":\"cumulus:offset\",\"properties\":{\"x\":\"2\",\"z\":\"-1\"}}],\"points\":[{\"id\":\"controller\",\"location\":\"0,0,0\",\"target\":\"cumulus:block\",\"target_properties\":{},\"predicates\":[],\"requirement\":\"REQUIRED\",\"captures\":[],\"groups\":[]},{\"id\":\"door\",\"location\":\"-1,0,0\",\"target\":\"cumulus:region\",\"target_properties\":{\"from\":\"-1,0,0\",\"to\":\"1,2,0\"},\"predicates\":[],\"requirement\":\"REQUIRED\",\"captures\":[],\"groups\":[]}],\"components\":[{\"name\":\"leaf\",\"type\":\"cumulus:substructure\",\"properties\":{\"anchor_offset\":\"0,1,0\",\"structure\":\"cumulus:door_leaf\"}}],\"versions\":[{\"version\":1,\"definition\":\"cumulus:machine_v1\"},{\"version\":2,\"definition\":\"cumulus:machine\"}],\"migrations\":[{\"from\":1,\"to\":2,\"id\":\"cumulus:machine_upgrade\",\"properties\":{\"preserve_context\":\"true\",\"strategy\":\"replace\"}}]},\"debug_asset\":true,\"runtime_plan\":{\"definition\":\"cumulus:machine\",\"entry_count\":2,\"entries\":[{\"id\":\"controller\",\"target\":\"cumulus:block\",\"min\":\"0,0,0\",\"max\":\"0,0,0\",\"properties\":{}},{\"id\":\"door\",\"target\":\"cumulus:region\",\"min\":\"-1,0,0\",\"max\":\"1,2,0\",\"properties\":{\"from\":\"-1,0,0\",\"to\":\"1,2,0\"}}],\"regions\":[{\"id\":\"door\",\"min\":\"-1,0,0\",\"max\":\"1,2,0\"}]}}",
                exported.serialized());
    }

    private static Map<String, Object> orderedMap(Object... entries) {
        var values = new LinkedHashMap<String, Object>();
        for (var index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], entries[index + 1]);
        }
        return values;
    }
}
