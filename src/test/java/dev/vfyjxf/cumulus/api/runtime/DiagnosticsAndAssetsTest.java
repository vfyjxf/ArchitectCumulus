package dev.vfyjxf.cumulus.api.runtime;

import dev.vfyjxf.cumulus.api.datagen.DataDrivenStructureAsset;
import dev.vfyjxf.cumulus.api.datagen.StructureDatagenSpec;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.FixHint;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.visualization.VisualizationElement;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagnosticsAndAssetsTest {
    @Test
    void diagnosticGraphCopiesInputsAndSupportsNestedCauses() {
        var descriptors = new HashMap<String, String>();
        descriptors.put("expected", "minecraft:stone");
        var evidence = DiagnosticEvidence.relative(
                RelativeBlockPos.of(1, 2, 3),
                "controller",
                "cumulus:block_id",
                "frame/top",
                "chunk section not loaded",
                descriptors);
        descriptors.put("actual", "minecraft:air");

        var fixProperties = new HashMap<String, String>();
        fixProperties.put("block", "minecraft:stone");
        var fix = new FixHint("place_block", "controller", "frame/top", fixProperties);
        fixProperties.put("ignored", "true");

        var nodes = new ArrayList<DiagnosticNode>();
        nodes.add(new DiagnosticNode(
                "missing-casing",
                Severity.ERROR,
                "Casing is missing",
                "diagnostic.cumulus.missing_casing",
                List.of(evidence),
                "definitions/machine.json",
                List.of(fix)));
        nodes.add(new DiagnosticNode(
                "root",
                Severity.WARN,
                "Machine is incomplete",
                "diagnostic.cumulus.machine_incomplete",
                List.of(),
                "definitions/machine.json",
                List.of()));

        var edges = new ArrayList<DiagnosticEdge>();
        edges.add(new DiagnosticEdge("root", "missing-casing", "caused_by"));

        var graph = new DiagnosticGraph("root", nodes, edges);
        nodes.clear();
        edges.clear();

        assertEquals("root", graph.rootNodeId());
        assertEquals(2, graph.nodes().size());
        assertEquals("missing-casing", graph.childrenOf("root").getFirst().id());
        assertEquals(Map.of("expected", "minecraft:stone"), evidence.descriptors());
        assertEquals(Map.of("block", "minecraft:stone"), fix.properties());
        assertThrows(UnsupportedOperationException.class, () -> graph.nodes().add(graph.nodes().getFirst()));
        assertThrows(UnsupportedOperationException.class, () -> evidence.descriptors().put("mutated", "true"));
        assertThrows(UnsupportedOperationException.class, () -> fix.properties().put("mutated", "true"));
    }

    @Test
    void visualizationModelCopiesRegionElements() {
        var elements = new ArrayList<VisualizationElement>();
        elements.add(VisualizationElement.region(
                "missing-casing-overlay",
                "block-region",
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(2, 1, 2),
                Severity.ERROR,
                "casing",
                "visualization.cumulus.missing_casing",
                100));

        var model = new VisualizationModel("machine-diagnostics", elements);
        elements.clear();

        assertEquals(1, model.elements().size());
        assertEquals(RelativeBlockPos.of(0, 0, 0), model.elements().getFirst().region().min());
        assertEquals(RelativeBlockPos.of(2, 1, 2), model.elements().getFirst().region().max());
        assertThrows(UnsupportedOperationException.class, () -> model.elements().clear());
    }

    @Test
    void datagenSpecExposesStableResourcePath() {
        var asset = new DataDrivenStructureAsset(
                ResourceLocation.parse("cumulus:machines/furnace_core"),
                1,
                ResourceLocation.parse("cumulus:furnace_core"),
                true);
        var definitions = new HashMap<ResourceLocation, DataDrivenStructureAsset>();
        definitions.put(asset.runtimeAssetId(), asset);

        var spec = new StructureDatagenSpec("cumulus", "machines/furnace_core", definitions);
        definitions.clear();

        assertEquals("data/cumulus/cumulus/structures/machines/furnace_core.json", asset.resourcePath());
        assertEquals("data/cumulus/cumulus/structures/machines/furnace_core.json", spec.resourcePath());
        assertEquals(1, spec.assets().size());
        assertThrows(UnsupportedOperationException.class, () -> spec.assets().clear());
    }
}
