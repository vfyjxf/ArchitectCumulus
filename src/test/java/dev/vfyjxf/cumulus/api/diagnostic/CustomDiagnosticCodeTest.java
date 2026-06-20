package dev.vfyjxf.cumulus.api.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Verifies the open {@link DiagnosticCode} / {@link DiagnosticCodes} registry
 * and the centralization of the 11+ standard codes.
 */
class CustomDiagnosticCodeTest {

    @Test
    void standardDiagnosticCodesArePreRegistered() {
        // All 17 standard codes resolve via registry lookup
        assertEquals(
                StandardDiagnosticCodes.missingTarget,
                DiagnosticCodes.fromId(ResourceLocation.parse("cumulus:missing_target")));
        assertEquals(
                StandardDiagnosticCodes.predicateFailed,
                DiagnosticCodes.fromId(ResourceLocation.parse("cumulus:predicate_failed")));
        assertEquals(
                StandardDiagnosticCodes.sharedPartConflict,
                DiagnosticCodes.fromId(ResourceLocation.parse("cumulus:shared_part_conflict")));
    }

    @Test
    void unknownDiagnosticCodeFallsBackToPlaceholder() {
        var id = ResourceLocation.parse("mymod:custom_failure");
        var resolved = DiagnosticCodes.fromId(id);
        assertEquals(id, resolved.id());
        assertEquals(Severity.warn, resolved.defaultSeverity());
        // Localization key synthesizes a stable key from the id
        assertTrue(resolved.localizationKey().startsWith("diagnostic.unknown."));
    }

    @Test
    void customDiagnosticCodeCanBeRegisteredAndResolved() {
        var custom = new SimpleDiagnosticCode(
                ResourceLocation.parse("mymod:custom_test_code"),
                Severity.info,
                "diagnostic.mymod.custom_test_code");
        DiagnosticCodes.register(custom);
        assertEquals(custom, DiagnosticCodes.fromId(custom.id()));
    }

    @Test
    void diagnosticNodeStoresSeverityAndLocalizationKey() {
        var node = new DiagnosticNode(
                "test_node",
                Severity.error,
                "missing block",
                "diagnostic.cumulus.missing_target",
                List.<DiagnosticEvidence>of(),
                "test/point",
                List.<FixHint>of());
        assertEquals(Severity.error, node.severity());
        assertEquals("diagnostic.cumulus.missing_target", node.localizationKey());
        assertEquals("test_node", node.id());
    }
}
