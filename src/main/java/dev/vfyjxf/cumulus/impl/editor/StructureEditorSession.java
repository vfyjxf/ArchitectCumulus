package dev.vfyjxf.cumulus.impl.editor;

import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public record StructureEditorSession(
        StructureRuntimePlan plan,
        BlockPos anchor,
        Optional<String> selectedElementId) {
    public StructureEditorSession {
        if (plan == null) {
            throw new IllegalArgumentException("Editor runtime plan must not be null");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("Editor anchor must not be null");
        }
        if (selectedElementId == null) {
            throw new IllegalArgumentException("Selected element optional must not be null");
        }
    }

    public static StructureEditorSession start(StructureRuntimePlan plan, BlockPos anchor) {
        return new StructureEditorSession(plan, anchor, Optional.empty());
    }

    public StructureEditorSession moveAnchor(BlockPos anchor) {
        return new StructureEditorSession(plan, anchor, selectedElementId);
    }

    public StructureEditorSession select(String elementId) {
        if (elementId == null || elementId.isBlank()) {
            throw new IllegalArgumentException("Selected element id must not be blank");
        }
        var known = plan.entries().stream().anyMatch(entry -> entry.id().equals(elementId));
        if (!known) {
            throw new IllegalArgumentException("Unknown preview element id: " + elementId);
        }
        return new StructureEditorSession(plan, anchor, Optional.of(elementId));
    }

    public StructureEditorSession clearSelection() {
        return new StructureEditorSession(plan, anchor, Optional.empty());
    }

    public GhostPreviewModel preview() {
        return GhostPreviewCompiler.fromPlan(plan, anchor, selectedElementId.orElse(null));
    }
}
