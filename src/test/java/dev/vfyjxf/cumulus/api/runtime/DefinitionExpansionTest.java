package dev.vfyjxf.cumulus.api.runtime;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.context.ContextSources;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.migration.MigrationDescriptor;
import dev.vfyjxf.cumulus.api.migration.StructureVersionDescriptor;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefinitionExpansionTest {
    @Test
    void targetsCoverNonBlockAndContextQueries() {
        assertEquals(ResourceLocation.parse("cumulus:block_entity"), Targets.blockEntity().target().typeId());
        assertEquals(ResourceLocation.parse("cumulus:entity"), Targets.entity().target().typeId());
        assertEquals(ResourceLocation.parse("cumulus:fluid"), Targets.fluid().target().typeId());
        assertEquals(ResourceLocation.parse("cumulus:context"), Targets.context("facing").target().typeId());
        assertEquals("facing", Targets.context("facing").target().properties().get("key"));
    }

    @Test
    void regionTargetStoresInclusiveBoundsAndRejectsInvertedBounds() {
        var from = RelativeBlockPos.of(-1, 0, -2);
        var to = RelativeBlockPos.of(1, 2, 2);
        var region = Targets.region(from, to);

        assertEquals(ResourceLocation.parse("cumulus:region"), region.target().typeId());
        assertEquals("-1,0,-2", region.target().properties().get("from"));
        assertEquals("1,2,2", region.target().properties().get("to"));

        assertThrows(IllegalArgumentException.class, () -> Targets.region(to, from));
    }

    @Test
    void regionComponentEmitsSingleRegionPoint() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:door"))
                .component(Components.region("door_frame",
                        RelativeBlockPos.of(-1, 0, 0),
                        RelativeBlockPos.of(1, 2, 0)))
                .build();

        assertEquals(1, definition.points().size());
        var point = definition.points().get(0);
        assertEquals("door_frame", point.id().value());
        assertEquals(ResourceLocation.parse("cumulus:region"), point.target().typeId());
        assertEquals("-1,0,0", point.target().properties().get("from"));
        assertEquals("1,2,0", point.target().properties().get("to"));
    }

    @Test
    void substructureComponentMetadataIsPreservedByDefinition() {
        var structureId = ResourceLocation.parse("cumulus:door_leaf");
        var definition = Structures.define(ResourceLocation.parse("cumulus:double_door"))
                .component(Components.substructure("left_leaf", structureId, RelativeBlockPos.of(-1, 0, 0)))
                .build();

        assertEquals(0, definition.points().size());
        assertEquals(1, definition.components().size());
        var component = definition.components().get(0);
        assertEquals("left_leaf", component.name());
        assertEquals(ResourceLocation.parse("cumulus:substructure"), component.typeId());
        assertEquals(structureId, component.properties().get("structure"));
        assertEquals("-1,0,0", component.properties().get("anchor_offset"));
    }

    @Test
    void transformsExposeRuntimeGeometryActions() {
        assertEquals(ResourceLocation.parse("cumulus:horizontal_mirror"), Transforms.horizontalMirror().id());
        assertEquals(ResourceLocation.parse("cumulus:rotate_y_90"), Transforms.rotateY90().id());
        assertEquals(ResourceLocation.parse("cumulus:rotate_y_180"), Transforms.rotateY180().id());
        assertEquals(ResourceLocation.parse("cumulus:rotate_y_270"), Transforms.rotateY270().id());
        assertEquals(ResourceLocation.parse("cumulus:rotations_3d"), Transforms.rotations3d().id());
        assertEquals("strict", Transforms.custom(ResourceLocation.parse("cumulus:custom"), Map.of("mode", "strict"))
                .properties()
                .get("mode"));
    }

    @Test
    void builderPreservesContextAndMigrationDescriptors() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:migrating_machine"))
                .context(ContextRuleDescriptor.derive(ContextKey.string("facing"), ContextSources.capture("controller").property("facing")))
                .version(new StructureVersionDescriptor(2, ResourceLocation.parse("cumulus:machine/v2")))
                .migration(new MigrationDescriptor(
                        1,
                        2,
                        ResourceLocation.parse("cumulus:migrations/machine_1_to_2"),
                        Map.of("context", "facing")))
                .build();

        assertEquals(1, definition.contextRules().size());
        assertEquals("facing", definition.contextRules().get(0).targetKey().name());
        assertEquals("controller", ((dev.vfyjxf.cumulus.api.context.sources.CapturePropertySource) definition.contextRules().get(0).source()).point().value());
        assertEquals("facing", ((dev.vfyjxf.cumulus.api.context.sources.CapturePropertySource) definition.contextRules().get(0).source()).property());

        assertEquals(1, definition.versions().size());
        assertEquals(2, definition.versions().get(0).version());
        assertEquals(ResourceLocation.parse("cumulus:machine/v2"), definition.versions().get(0).definitionId());

        assertEquals(1, definition.migrations().size());
        assertEquals(1, definition.migrations().get(0).fromVersion());
        assertEquals(2, definition.migrations().get(0).toVersion());
        assertEquals(ResourceLocation.parse("cumulus:migrations/machine_1_to_2"),
                definition.migrations().get(0).migrationId());
        assertEquals("facing", definition.migrations().get(0).properties().get("context"));
    }
}
