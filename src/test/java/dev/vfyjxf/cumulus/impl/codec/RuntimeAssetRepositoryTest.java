package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;
import dev.vfyjxf.cumulus.impl.RuntimePlanEntry;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeAssetRepositoryTest {
    @Test
    void indexesValidRuntimeAssetsByAssetAndDefinitionId() {
        var repository = new RuntimeAssetRepository();
        var asset = asset("cumulus:runtime_asset", "cumulus:definition");

        repository.apply(new RuntimeAssetLoadResult(List.of(asset), emptyDiagnostics()));

        assertEquals(asset, repository.byId(ResourceLocation.parse("cumulus:runtime_asset")).orElseThrow());
        assertEquals(List.of(asset), repository.byDefinition(ResourceLocation.parse("cumulus:definition")));
        assertEquals(List.of(asset), repository.assets());
        assertTrue(repository.lastResult().orElseThrow().valid());
    }

    @Test
    void invalidReloadPreservesPreviousValidSnapshotAndStoresDiagnostics() {
        var repository = new RuntimeAssetRepository();
        var asset = asset("cumulus:runtime_asset", "cumulus:definition");
        repository.apply(new RuntimeAssetLoadResult(List.of(asset), emptyDiagnostics()));

        repository.apply(new RuntimeAssetLoadResult(List.of(), errorDiagnostics("INVALID_JSON")));

        assertEquals(List.of(asset), repository.assets());
        assertFalse(repository.lastResult().orElseThrow().valid());
        assertEquals("INVALID_JSON", repository.lastResult().orElseThrow().diagnostics().nodes().stream().filter(node -> node.severity() == Severity.error).findFirst().orElseThrow().id());
    }

    private static LoadedRuntimeAsset asset(String id, String definitionId) {
        var definition = ResourceLocation.parse(definitionId);
        return new LoadedRuntimeAsset(
                        "data/cumulus/cumulus/structures/" + ResourceLocation.parse(id).getPath() + ".json",
                        ResourceLocation.parse(id),
                        1,
                        definition,
                        false,
                        new StructureRuntimePlan(definition, List.<RuntimePlanEntry>of(), List.of(), List.of(),
                                        new VisualizationModel(definitionId, List.of())),
                        List.of());
    }

    private static DiagnosticGraph emptyDiagnostics() {
        return new DiagnosticGraph("root", List.of(), List.of());
    }

    private static DiagnosticGraph errorDiagnostics(String id) {
        return new DiagnosticGraph(
                        "root",
                        List.of(new DiagnosticNode(
                                        id,
                                        Severity.error,
                                        id,
                                        "diagnostic.cumulus.test",
                                        List.of(),
                                        "test",
                                        List.of())),
                        List.of());
    }
}
