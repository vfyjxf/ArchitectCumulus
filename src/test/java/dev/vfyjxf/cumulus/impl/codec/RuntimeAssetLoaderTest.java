package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.diagnostic.Severity;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeAssetLoaderTest {
    @Test
    void loadsSerializedRuntimeAssetMapsInDeterministicPathOrder() {
        var loader = new RuntimeAssetLoader();
        var assets = loader.load(List.of(source("data/cumulus/cumulus/structures/b.json", asset("cumulus:b", "cumulus:b_def")), source("data/cumulus/cumulus/structures/a.json", asset("cumulus:a", "cumulus:a_def"))));

        assertTrue(assets.valid());
        assertEquals(List.of("data/cumulus/cumulus/structures/a.json", "data/cumulus/cumulus/structures/b.json"), assets.assets().stream().map(LoadedRuntimeAsset::resourcePath).toList());
        assertEquals("cumulus:a", assets.assets().get(0).id().toString());
        assertEquals(1, assets.assets().get(0).schemaVersion());
        assertEquals("cumulus:a_def", assets.assets().get(0).definitionId().toString());
        assertFalse(assets.assets().get(0).debugAsset());
        assertEquals(1, assets.assets().get(0).runtimePlan().entries().size());
    }

    @Test
    void rejectsDuplicateResourcePathsWithDiagnostic() {
        var loader = new RuntimeAssetLoader();

        var result = loader.load(List.of(source("data/cumulus/cumulus/structures/duplicate.json", asset("cumulus:first", "cumulus:first")), source("data/cumulus/cumulus/structures/duplicate.json", asset("cumulus:second", "cumulus:second"))));

        assertFalse(result.valid());
        assertEquals(List.of("DUPLICATE_RESOURCE_PATH"), result.diagnostics().nodes().stream().filter(node -> node.severity() == Severity.error).map(node -> node.id()).toList());
    }

    @Test
    void loadsSerializedPredicateDescriptorsFromAssets() {
        var values = asset("cumulus:predicate_machine", "cumulus:predicate_machine");
        values.put("predicates", List.of(Map.of("id", "cumulus:custom", "mode", "capability", "name", "cumulus:item_handler")));

        var result = new RuntimeAssetLoader().load(List.of(source("data/cumulus/cumulus/structures/predicate_machine.json", values)));

        assertTrue(result.valid());
        var predicate = result.assets().getFirst().predicates().getFirst();
        assertEquals("cumulus:custom", predicate.id().toString());
        assertEquals("capability", predicate.properties().get("mode"));
        assertEquals("cumulus:item_handler", predicate.properties().get("name"));
    }

    @Test
    void missingRequiredFieldsProduceStableErrorDiagnostics() {
        var values = new LinkedHashMap<String, Object>();
        values.put("schema_version", 1);

        var result = new RuntimeAssetLoader().load(List.of(source("data/cumulus/cumulus/structures/broken.json", values)));

        assertFalse(result.valid());
        assertEquals(List.of("MISSING_ID", "MISSING_DEFINITION", "MISSING_DEBUG_ASSET", "MISSING_RUNTIME_PLAN"), result.diagnostics().nodes().stream().filter(node -> node.severity() == Severity.error).map(node -> node.id()).toList());
    }

    @Test
    void invalidPredicateMapsProduceDiagnosticsInsteadOfThrowing() {
        var values = asset("cumulus:bad_predicate", "cumulus:bad_predicate");
        values.put("predicates", List.of(Map.of("name", "missing id")));

        var result = new RuntimeAssetLoader().load(List.of(source("data/cumulus/cumulus/structures/bad_predicate.json", values)));

        assertFalse(result.valid());
        assertEquals(List.of("INVALID_PREDICATE"), result.diagnostics().nodes().stream().filter(node -> node.severity() == Severity.error).map(node -> node.id()).toList());
    }

    private static RuntimeAssetSource source(String resourcePath, Map<String, Object> values) {
        return new RuntimeAssetSource(resourcePath, values);
    }

    private static Map<String, Object> asset(String id, String definitionId) {
        var values = new LinkedHashMap<String, Object>();
        values.put("id", id);
        values.put("schema_version", 1);
        values.put("definition", definitionId);
        values.put("debug_asset", false);
        values.put("runtime_plan", Map.of("definition", definitionId, "entry_count", 1, "entries", List.of(Map.of("id", "origin", "target", "cumulus:block", "min", "0,0,0", "max", "0,0,0")), "regions", List.of()));
        return values;
    }
}
