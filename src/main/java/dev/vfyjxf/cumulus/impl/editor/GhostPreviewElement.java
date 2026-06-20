package dev.vfyjxf.cumulus.impl.editor;

import dev.vfyjxf.cumulus.api.diagnostic.Severity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record GhostPreviewElement(
                String id,
                ResourceLocation targetType,
                BlockPos min,
                BlockPos max,
                Map<String, String> properties,
                Severity severity,
                int priority) {
    public GhostPreviewElement {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Ghost preview element id must not be blank");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Ghost preview target type must not be null");
        }
        if (min == null || max == null) {
            throw new IllegalArgumentException("Ghost preview bounds must not be null");
        }
        properties = Map.copyOf(properties);
        if (severity == null) {
            throw new IllegalArgumentException("Ghost preview severity must not be null");
        }
    }
}
