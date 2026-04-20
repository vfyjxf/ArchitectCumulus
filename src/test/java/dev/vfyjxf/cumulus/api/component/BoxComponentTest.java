package dev.vfyjxf.cumulus.api.component;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.Targets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoxComponentTest {
    @Test
    void filledBoxExpandsAllPositionsInclusively() {
        var component = Components.box("casing",
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(1, 1, 1))
            .target(Targets.block())
            .where(PredicateDescriptors.id("cumulus:any"));

        var emitted = new ArrayList<StructurePointDefinition>();
        component.emit(emitted::add);

        assertEquals(8, emitted.size());
        assertEquals(RelativeBlockPos.of(0, 0, 0), emitted.get(0).location());
        assertEquals(RelativeBlockPos.of(1, 1, 1), emitted.get(7).location());
    }

    @Test
    void hollowBoxOnlyEmitsShell() {
        var component = Components.hollowBox("shell",
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(2, 2, 2))
            .target(Targets.block());

        var emitted = new ArrayList<StructurePointDefinition>();
        component.emit(emitted::add);

        assertEquals(26, emitted.size());
    }

    @Test
    void boxRejectsInvertedBounds() {
        assertThrows(IllegalArgumentException.class, () ->
                Components.box("bad", RelativeBlockPos.of(1, 0, 0), RelativeBlockPos.of(0, 0, 0)));
    }
}
