package dev.vfyjxf.cumulus.impl.editor;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GhostPreviewCompilerTest {
    @Test
    void previewTranslatesRuntimePlanEntriesToAbsoluteRegions() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:preview_machine"))
                .point("controller", RelativeBlockPos.of(1, 0, -1), Targets.block()
                        .property("block", "minecraft:furnace"))
                .component(Components.region(
                                "frame",
                                RelativeBlockPos.of(0, 0, 0),
                                RelativeBlockPos.of(1, 1, 0))
                        .target(Targets.region(
                                        RelativeBlockPos.of(0, 0, 0),
                                        RelativeBlockPos.of(1, 1, 0))
                                .property("block", "minecraft:stone")))
                .build();
        var plan = StructureRuntimePlanCompiler.compile(definition);

        var preview = GhostPreviewCompiler.fromPlan(plan, new BlockPos(10, 64, 10));

        assertEquals("cumulus:preview_machine/ghost", preview.id());
        assertEquals(new BlockPos(10, 64, 10), preview.anchor());
        assertEquals(2, preview.elements().size());
        var controller = preview.elements().getFirst();
        assertEquals("controller", controller.id());
        assertEquals(ResourceLocation.parse("cumulus:block"), controller.targetType());
        assertEquals(new BlockPos(11, 64, 9), controller.min());
        assertEquals(new BlockPos(11, 64, 9), controller.max());
        assertEquals("minecraft:furnace", controller.properties().get("block"));
        var frame = preview.elements().get(1);
        assertEquals(new BlockPos(10, 64, 10), frame.min());
        assertEquals(new BlockPos(11, 65, 10), frame.max());
        assertEquals("minecraft:stone", frame.properties().get("block"));
    }

    @Test
    void editorSessionSelectionAndAnchorAreReflectedInPreview() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:editable_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .point("port", RelativeBlockPos.of(2, 0, 0), Targets.block())
                .build();
        var plan = StructureRuntimePlanCompiler.compile(definition);

        var session = StructureEditorSession.start(plan, BlockPos.ZERO)
                .select("port")
                .moveAnchor(new BlockPos(5, 70, 5));
        var preview = session.preview();

        assertEquals("port", session.selectedElementId().orElseThrow());
        assertEquals(new BlockPos(5, 70, 5), preview.anchor());
        var selected = preview.elements().stream()
                .filter(element -> element.id().equals("port"))
                .findFirst()
                .orElseThrow();
        assertEquals(new BlockPos(7, 70, 5), selected.min());
        assertEquals(Severity.warn, selected.severity());
        assertEquals(100, selected.priority());
    }

    @Test
    void renderPlanOrdersSelectedElementsLastAndMapsSeverityToColors() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:renderable_preview"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .point("port", RelativeBlockPos.of(2, 0, 0), Targets.block())
                .build();
        var preview = StructureEditorSession.start(
                        StructureRuntimePlanCompiler.compile(definition),
                        new BlockPos(4, 64, 4))
                .select("port")
                .preview();

        var renderPlan = GhostPreviewRenderPlan.fromPreview(preview);

        assertEquals(2, renderPlan.commands().size());
        assertEquals(List.of("controller", "port"),
                renderPlan.commands().stream().map(GhostPreviewRenderCommand::elementId).toList());
        var normal = renderPlan.commands().getFirst();
        assertEquals(GhostPreviewRenderStyle.info, normal.style());
        assertEquals(0.25F, normal.alpha());
        var selected = renderPlan.commands().get(1);
        assertEquals(GhostPreviewRenderStyle.selected, selected.style());
        assertEquals(1.0F, selected.red());
        assertEquals(0.72F, selected.green());
        assertEquals(0.15F, selected.blue());
        assertEquals(0.72F, selected.alpha());
    }

    @Test
    void editorControllerTogglesSessionsAndCyclesSelection() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:controlled_preview"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .point("port", RelativeBlockPos.of(2, 0, 0), Targets.block())
                .build();
        var plan = StructureRuntimePlanCompiler.compile(definition);
        var controller = new StructureEditorController();

        controller.toggle(plan, new BlockPos(1, 65, 1));
        controller.selectNext();
        controller.selectNext();

        var session = controller.activeSession().orElseThrow();
        assertEquals("port", session.selectedElementId().orElseThrow());
        assertEquals(new BlockPos(1, 65, 1), session.anchor());
        assertEquals(2, controller.renderPlan().orElseThrow().commands().size());

        controller.toggle(plan, BlockPos.ZERO);

        assertTrue(controller.activeSession().isEmpty());
        assertTrue(controller.renderPlan().isEmpty());
    }
}
