package dev.vfyjxf.cumulus.impl;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureRuntimePlanCompilerTest {
    @Test
    void compilesDefinitionFootprintToRuntimeSectionsAndBuildHints() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:door"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().capture("controller"))
                .component(Components.region("door_frame", RelativeBlockPos.of(-1, 0, -1), RelativeBlockPos.of(17, 2, 1)))
                .component(Components.substructure(
                        "left_leaf",
                        ResourceLocation.parse("cumulus:door_leaf"),
                        RelativeBlockPos.of(-1, 0, 0)))
                .build();

        var plan = StructureRuntimePlanCompiler.compile(definition);

        assertEquals(definition.id(), plan.definitionId());
        assertEquals(2, plan.entries().size());
        assertEquals(1, plan.substructures().size());
        assertEquals(Set.of(
                        new SectionKey(-1, 4, -1),
                        new SectionKey(-1, 4, 0),
                        new SectionKey(0, 4, -1),
                        new SectionKey(0, 4, 0),
                        new SectionKey(1, 4, -1),
                        new SectionKey(1, 4, 0)),
                plan.footprintSections(new BlockPos(0, 64, 0)));
        assertEquals(2, plan.buildHints().size());
        assertEquals(2, plan.visualization().elements().size());
    }

    @Test
    void rotationDescriptorTransformsRelativeBlockPositions() {
        var rotated = StructureRuntimePlanCompiler.transform(
                RelativeBlockPos.of(1, 2, 3),
                Transforms.rotateY90());

        assertEquals(RelativeBlockPos.of(-3, 2, 1), rotated);
    }

    @Test
    void regionFootprintIncludesSectionsCrossedByInclusiveBounds() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:wide_region"))
                .component(Components.region("wide", RelativeBlockPos.of(0, 0, 0), RelativeBlockPos.of(32, 0, 0)))
                .build();

        var plan = StructureRuntimePlanCompiler.compile(definition);

        assertTrue(plan.footprintSections(new BlockPos(0, 0, 0))
                .containsAll(Set.of(new SectionKey(0, 0, 0), new SectionKey(1, 0, 0), new SectionKey(2, 0, 0))));
    }

    @Test
    void compilesTransformAwareCheckPlanEntries() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:compiled_check"))
                .transforms(Transforms.identity(), Transforms.rotateY90())
                .point("controller", RelativeBlockPos.of(1, 0, 0), Targets.block())
                .component(Components.region(
                        "door",
                        RelativeBlockPos.of(0, 0, 1),
                        RelativeBlockPos.of(2, 1, 1)))
                .build();

        var plan = StructureRuntimePlanCompiler.compileCheckPlan(definition);

        assertEquals(definition.id(), plan.definitionId());
        assertEquals(2, plan.candidates().size());
        assertEquals(RelativeBlockPos.of(1, 0, 0),
                plan.candidates().getFirst().entries().getFirst().min());
        assertEquals(RelativeBlockPos.of(0, 0, 1),
                plan.candidates().get(1).entries().getFirst().min());
        assertEquals(RelativeBlockPos.of(-1, 0, 0),
                plan.candidates().get(1).entries().get(1).min());
        assertEquals(RelativeBlockPos.of(-1, 1, 2),
                plan.candidates().get(1).entries().get(1).max());
    }
}
