package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.datagen.DataDrivenStructureAsset;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import dev.vfyjxf.cumulus.impl.RuntimeAssetExporter;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeAssetJsonLoaderTest {
    @Test
    void loadsSerializedRuntimeAssetExporterContent() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:json_machine")).point("origin", RelativeBlockPos.of(0, 0, 0), Targets.block()).build();
        var exported = RuntimeAssetExporter.export(new DataDrivenStructureAsset(
                        ResourceLocation.parse("cumulus:json_machine"),
                        1,
                        definition.id(),
                        false), StructureRuntimePlanCompiler.compile(definition));

        var result = new RuntimeAssetJsonLoader().load(List.of(new RuntimeAssetJsonSource(
                        exported.resourcePath(),
                        exported.serialized())));

        assertTrue(result.valid());
        assertEquals("cumulus:json_machine", result.assets().getFirst().id().toString());
        assertEquals("origin", result.assets().getFirst().runtimePlan().entries().getFirst().id());
    }

    @Test
    void loadsExporterEmbeddedDefinitionsForRuntimeRegistration() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:embedded_machine")).transforms(Transforms.identity(), Transforms.rotateY90()).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller")).capture("controller")).point("tank", RelativeBlockPos.of(1, 0, 0), Targets.fluid().where(PredicateDescriptors.id("minecraft:water"))).build();
        var exported = RuntimeAssetExporter.export(DataDrivenStructureAsset.fromDefinition(ResourceLocation.parse("cumulus:embedded_machine_asset"), 1, definition, false), StructureRuntimePlanCompiler.compile(definition));

        var result = new RuntimeAssetJsonLoader().load(List.of(new RuntimeAssetJsonSource(
                        exported.resourcePath(),
                        exported.serialized())));

        assertTrue(result.valid());
        var loaded = result.assets().getFirst();
        assertEquals(definition.id(), loaded.definitionId());
        assertTrue(loaded.definition().isPresent());
        var loadedDefinition = loaded.definition().orElseThrow();
        assertEquals(definition.id(), loadedDefinition.id());
        assertEquals(2, loadedDefinition.transforms().size());
        assertEquals(2, loadedDefinition.points().size());
        assertEquals("controller", loadedDefinition.points().getFirst().id().value());
        assertEquals("cumulus:controller", loadedDefinition.points().getFirst().predicates().getFirst().id().toString());
        assertEquals(List.of("controller"), loadedDefinition.points().getFirst().captures().stream().sorted().toList());
        assertEquals("tank", loadedDefinition.points().get(1).id().value());
        assertEquals("cumulus:fluid", loadedDefinition.points().get(1).target().typeId().toString());
    }

    @Test
    void invalidJsonProducesDiagnosticInsteadOfThrowing() {
        var result = new RuntimeAssetJsonLoader().load(List.of(new RuntimeAssetJsonSource(
                        "data/cumulus/cumulus/structures/broken.json",
                        "{\"id\":\"cumulus:broken\"")));

        assertFalse(result.valid());
        assertEquals(List.of("INVALID_JSON"), result.diagnostics().nodes().stream().filter(node -> node.severity() == Severity.error).map(node -> node.id()).toList());
    }
}
