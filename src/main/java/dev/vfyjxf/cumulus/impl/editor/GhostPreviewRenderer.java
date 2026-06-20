package dev.vfyjxf.cumulus.impl.editor;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class GhostPreviewRenderer {
    public void render(GhostPreviewRenderPlan plan, PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        if (plan == null) {
            throw new IllegalArgumentException("Ghost preview render plan must not be null");
        }
        if (poseStack == null) {
            throw new IllegalArgumentException("Ghost preview pose stack must not be null");
        }
        if (bufferSource == null) {
            throw new IllegalArgumentException("Ghost preview buffer source must not be null");
        }
        if (camera == null) {
            throw new IllegalArgumentException("Ghost preview camera must not be null");
        }
        var cameraPosition = camera.getPosition();
        var buffer = bufferSource.getBuffer(RenderType.lines());
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x(), -cameraPosition.y(), -cameraPosition.z());
        for (var command : plan.commands()) {
            LevelRenderer.renderLineBox(poseStack, buffer, command.bounds(), command.red(), command.green(), command.blue(), command.alpha());
        }
        poseStack.popPose();
        if (bufferSource instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(RenderType.lines());
        }
    }
}
