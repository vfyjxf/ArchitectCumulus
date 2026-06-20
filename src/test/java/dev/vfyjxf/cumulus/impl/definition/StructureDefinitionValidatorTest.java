package dev.vfyjxf.cumulus.impl.definition;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinitionException;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.target.Targets;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureDefinitionValidatorTest {
    @Test
    void rejectsDuplicatePointIds() {
        var thrown = assertThrows(StructureDefinitionException.class, () -> Structures.define(ResourceLocation.parse("cumulus:bad")).point("same", RelativeBlockPos.of(0, 0, 0), Targets.block()).point("same", RelativeBlockPos.of(1, 0, 0), Targets.block()).build());

        assertTrue(thrown.getMessage().contains("same"));
        assertTrue(thrown.getMessage().contains("0,0,0"));
        assertTrue(thrown.getMessage().contains("1,0,0"));
    }

    @Test
    void rejectsDuplicateCaptureNames() {
        var thrown = assertThrows(StructureDefinitionException.class, () -> Structures.define(ResourceLocation.parse("cumulus:bad")).point("a", RelativeBlockPos.of(0, 0, 0), Targets.block().capture("controller")).point("b", RelativeBlockPos.of(1, 0, 0), Targets.block().capture("controller")).build());

        assertTrue(thrown.getMessage().contains("controller"));
        assertTrue(thrown.getMessage().contains("a"));
        assertTrue(thrown.getMessage().contains("b"));
        assertTrue(thrown.getMessage().contains("0,0,0"));
        assertTrue(thrown.getMessage().contains("1,0,0"));
    }

    @Test
    void rejectsRequiredSpatialTargetConflict() {
        var thrown = assertThrows(StructureDefinitionException.class, () -> Structures.define(ResourceLocation.parse("cumulus:bad")).point("a", RelativeBlockPos.of(0, 0, 0), Targets.block()).point("b", RelativeBlockPos.of(0, 0, 0), Targets.block()).build());

        assertTrue(thrown.getMessage().contains("a"));
        assertTrue(thrown.getMessage().contains("b"));
        assertTrue(thrown.getMessage().contains("0,0,0"));
    }

    @Test
    void ignoresOptionalPointsForSpatialTargetConflicts() {
        assertDoesNotThrow(() -> Structures.define(ResourceLocation.parse("cumulus:ok")).point("required", RelativeBlockPos.of(0, 0, 0), Targets.block()).component(Components.point("optional", RelativeBlockPos.of(0, 0, 0)).optional()).build());
    }

    @Test
    void rejectsNullDefinition() {
        assertThrows(IllegalArgumentException.class, () -> StructureDefinitionValidator.validate(null));
    }
}
