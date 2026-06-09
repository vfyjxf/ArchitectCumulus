package dev.vfyjxf.cumulus.impl.editor;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cumulus.Cumulus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class StructureEditorClientBridge {
    private static final String CATEGORY = "key.categories." + Cumulus.MODID + ".editor";

    private final StructureEditorController controller;
    private final GhostPreviewRenderer renderer;
    private final KeyMapping selectNextKey = new KeyMapping(
            "key." + Cumulus.MODID + ".editor.select_next",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_RIGHT,
            CATEGORY);
    private final KeyMapping closeKey = new KeyMapping(
            "key." + Cumulus.MODID + ".editor.close",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_ESCAPE,
            CATEGORY);

    public StructureEditorClientBridge(StructureEditorController controller) {
        this(controller, new GhostPreviewRenderer());
    }

    StructureEditorClientBridge(
            StructureEditorController controller,
            GhostPreviewRenderer renderer) {
        if (controller == null) {
            throw new IllegalArgumentException("Structure editor controller must not be null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("Ghost preview renderer must not be null");
        }
        this.controller = controller;
        this.renderer = renderer;
    }

    public void register(IEventBus modEventBus, IEventBus gameEventBus) {
        if (modEventBus == null) {
            throw new IllegalArgumentException("Mod event bus must not be null");
        }
        if (gameEventBus == null) {
            throw new IllegalArgumentException("Game event bus must not be null");
        }
        modEventBus.addListener(this::registerKeyMappings);
        gameEventBus.addListener(this::onClientTick);
        gameEventBus.addListener(this::onRenderLevelStage);
    }

    void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(selectNextKey);
        event.register(closeKey);
    }

    void onClientTick(ClientTickEvent.Post event) {
        while (selectNextKey.consumeClick()) {
            controller.selectNext();
        }
        while (closeKey.consumeClick()) {
            controller.stop();
        }
    }

    void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        controller.renderPlan().ifPresent(plan -> renderer.render(
                plan,
                event.getPoseStack(),
                minecraft.renderBuffers().bufferSource(),
                event.getCamera()));
    }
}
