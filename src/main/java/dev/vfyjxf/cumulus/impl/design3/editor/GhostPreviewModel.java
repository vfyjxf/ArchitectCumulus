package dev.vfyjxf.cumulus.impl.design3.editor;

import java.util.List;
import net.minecraft.core.BlockPos;

public record GhostPreviewModel(
        String id,
        BlockPos anchor,
        List<GhostPreviewElement> elements) {
    public GhostPreviewModel {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Ghost preview id must not be blank");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("Ghost preview anchor must not be null");
        }
        elements = List.copyOf(elements);
    }
}
