package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructureDefinitionBuilderTest {
    @Test
    void defaultsToIdentityTransform() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:marker")).point("origin", RelativeBlockPos.of(0, 0, 0), Targets.block()).build();

        assertEquals(1, definition.transforms().size());
        assertEquals(Transforms.identity(), definition.transforms().get(0));
    }

    @Test
    void dslBuildsPointsComponentsCapturesAndTransforms() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine")).transforms(Transforms.identity(), Transforms.horizontalFacing()).point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller")).capture("controller")).component(Components.box("casing", RelativeBlockPos.of(-1, 1, -1), RelativeBlockPos.of(1, 1, 1)).target(Targets.block()).where(PredicateDescriptors.id("cumulus:casing"))).build();

        assertEquals(ResourceLocation.parse("cumulus:machine"), definition.id());
        assertEquals(2, definition.transforms().size());
        assertEquals(10, definition.points().size());
        assertEquals("controller", definition.points().get(0).captures().iterator().next());
    }
}
