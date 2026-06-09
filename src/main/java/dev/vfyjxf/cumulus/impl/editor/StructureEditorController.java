package dev.vfyjxf.cumulus.impl.editor;

import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public final class StructureEditorController {
    private StructureEditorSession activeSession;

    public Optional<StructureEditorSession> activeSession() {
        return Optional.ofNullable(activeSession);
    }

    public void start(StructureRuntimePlan plan, BlockPos anchor) {
        activeSession = StructureEditorSession.start(plan, anchor);
    }

    public void stop() {
        activeSession = null;
    }

    public void toggle(StructureRuntimePlan plan, BlockPos anchor) {
        if (activeSession == null) {
            start(plan, anchor);
        } else {
            stop();
        }
    }

    public void moveAnchor(BlockPos anchor) {
        activeSession().ifPresent(session -> activeSession = session.moveAnchor(anchor));
    }

    public void selectNext() {
        activeSession().ifPresent(session -> activeSession = session.select(nextElementId(session)));
    }

    public Optional<GhostPreviewRenderPlan> renderPlan() {
        return activeSession().map(session -> GhostPreviewRenderPlan.fromPreview(session.preview()));
    }

    private static String nextElementId(StructureEditorSession session) {
        var entries = session.plan().entries();
        if (entries.isEmpty()) {
            throw new IllegalStateException("Cannot select an element from an empty editor plan");
        }
        var selected = session.selectedElementId();
        if (selected.isEmpty()) {
            return entries.getFirst().id();
        }
        for (var index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(selected.orElseThrow())) {
                return entries.get((index + 1) % entries.size()).id();
            }
        }
        return entries.getFirst().id();
    }
}
