package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.target.TargetDescriptor;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructureDefinitionTest {
    @Test
    void copiesMutableInputs() {
        var predicates = new java.util.ArrayList<PredicateDescriptor>();
        predicates.add(new PredicateDescriptor(ResourceLocation.parse("cumulus:any"), Map.of()));

        var point = new StructurePointDefinition(
                new StructurePointId("controller"),
                RelativeBlockPos.of(0, 0, 0),
                new TargetDescriptor(ResourceLocation.parse("cumulus:block"), Map.of()),
                predicates,
                PointRequirement.REQUIRED,
                Set.of("controller"),
                Set.of("core"),
                DiagnosticMetadata.empty());

        predicates.clear();

        assertEquals(1, point.predicates().size());
    }

    @Test
    void definitionHasSchemaVersionOne() {
        var definition = new StructureDefinition(
                ResourceLocation.parse("cumulus:test"),
                List.of(new TransformDescriptor(ResourceLocation.parse("cumulus:identity"), Map.of())),
                List.of(),
                List.of(),
                DiagnosticMetadata.empty());

        assertEquals(1, definition.schemaVersion());
    }

    @Test
    void pointIdRejectsBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new StructurePointId(" "));
    }
}
