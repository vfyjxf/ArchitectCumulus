package dev.vfyjxf.cumulus.impl.editor;

import dev.vfyjxf.cumulus.api.diagnostic.Severity;

import java.util.Comparator;
import java.util.List;

public record GhostPreviewRenderPlan(
                String previewId,
                List<GhostPreviewRenderCommand> commands) {
    public GhostPreviewRenderPlan {
        if (previewId == null || previewId.isBlank()) {
            throw new IllegalArgumentException("Ghost preview render plan id must not be blank");
        }
        commands = List.copyOf(commands);
    }

    public static GhostPreviewRenderPlan fromPreview(GhostPreviewModel preview) {
        if (preview == null) {
            throw new IllegalArgumentException("Ghost preview model must not be null");
        }
        var commands = preview.elements().stream().sorted(Comparator.comparingInt(GhostPreviewElement::priority).thenComparing(GhostPreviewElement::id)).map(GhostPreviewRenderPlan::command).toList();
        return new GhostPreviewRenderPlan(preview.id() + "/render", commands);
    }

    private static GhostPreviewRenderCommand command(GhostPreviewElement element) {
        var selected = element.priority() > 0;
        var style = selected ? GhostPreviewRenderStyle.selected : styleFor(element.severity());
        return switch (style) {
            case selected -> command(element, style, 1.0F, 0.72F, 0.15F, 0.72F);
            case error -> command(element, style, 1.0F, 0.18F, 0.18F, 0.5F);
            case warning -> command(element, style, 1.0F, 0.82F, 0.2F, 0.4F);
            case info -> command(element, style, 0.35F, 0.78F, 1.0F, 0.25F);
        };
    }

    private static GhostPreviewRenderCommand command(GhostPreviewElement element, GhostPreviewRenderStyle style, float red, float green, float blue, float alpha) {
        return new GhostPreviewRenderCommand(
                        element.id(),
                        element.min(),
                        element.max(),
                        style,
                        red,
                        green,
                        blue,
                        alpha);
    }

    private static GhostPreviewRenderStyle styleFor(Severity severity) {
        return switch (severity) {
            case error -> GhostPreviewRenderStyle.error;
            case warn -> GhostPreviewRenderStyle.warning;
            case info -> GhostPreviewRenderStyle.info;
        };
    }
}
