package dev.vfyjxf.cumulus.impl.editor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public record GhostPreviewRenderCommand(
                String elementId,
                BlockPos min,
                BlockPos max,
                GhostPreviewRenderStyle style,
                float red,
                float green,
                float blue,
                float alpha) {
    public GhostPreviewRenderCommand {
        if (elementId == null || elementId.isBlank()) {
            throw new IllegalArgumentException("Render command element id must not be blank");
        }
        if (min == null || max == null) {
            throw new IllegalArgumentException("Render command bounds must not be null");
        }
        if (style == null) {
            throw new IllegalArgumentException("Render command style must not be null");
        }
        requireUnit(red, "red");
        requireUnit(green, "green");
        requireUnit(blue, "blue");
        requireUnit(alpha, "alpha");
    }

    public AABB bounds() {
        var minX = Math.min(min.getX(), max.getX());
        var minY = Math.min(min.getY(), max.getY());
        var minZ = Math.min(min.getZ(), max.getZ());
        var maxX = Math.max(min.getX(), max.getX()) + 1;
        var maxY = Math.max(min.getY(), max.getY()) + 1;
        var maxZ = Math.max(min.getZ(), max.getZ()) + 1;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void requireUnit(float value, String name) {
        if (value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException("Render command " + name + " must be between 0 and 1");
        }
    }
}
