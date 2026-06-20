package dev.vfyjxf.cumulus.api.diagnostic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticMetadataTest {

    @Test
    void emptyIsIsEmpty() {
        assertTrue(DiagnosticMetadata.empty().isEmpty());
    }

    @Test
    void ofSinglePair() {
        var meta = DiagnosticMetadata.of("tier", "advanced");
        assertEquals("advanced", meta.get("tier").orElseThrow());
        assertTrue(meta.get("absent").isEmpty());
    }

    @Test
    void ofTwoPairs() {
        var meta = DiagnosticMetadata.of("tier", "advanced", "facing", "north");
        assertEquals("advanced", meta.get("tier").orElseThrow());
        assertEquals("north", meta.get("facing").orElseThrow());
    }

    @Test
    void builderAccumulates() {
        var meta = DiagnosticMetadata.builder().put("coil_type", "cupronickel").put("tier", "3").build();
        assertEquals("cupronickel", meta.get("coil_type").orElseThrow());
        assertEquals("3", meta.get("tier").orElseThrow());
        assertEquals(2, meta.properties().size());
    }
}
