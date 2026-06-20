package dev.vfyjxf.cumulus.impl.editor;

import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;

public final class GhostPreviewCompiler {
    private GhostPreviewCompiler() {
    }

    public static GhostPreviewModel fromPlan(StructureRuntimePlan plan, BlockPos anchor) {
        return fromPlan(plan, anchor, null);
    }

    public static GhostPreviewModel fromPlan(
            StructureRuntimePlan plan,
            BlockPos anchor,
            String selectedElementId) {
        if (plan == null) {
            throw new IllegalArgumentException("Runtime plan must not be null");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("Ghost preview anchor must not be null");
        }
        var elements = new ArrayList<GhostPreviewElement>();
        for (var entry : plan.entries()) {
            var selected = entry.id().equals(selectedElementId);
            elements.add(new GhostPreviewElement(
                    entry.id(),
                    entry.targetType(),
                    anchor.offset(entry.min().x(), entry.min().y(), entry.min().z()),
                    anchor.offset(entry.max().x(), entry.max().y(), entry.max().z()),
                    entry.properties(),
                    selected ? Severity.warn : Severity.info,
                    selected ? 100 : 0));
        }
        return new GhostPreviewModel(plan.definitionId() + "/ghost", anchor, elements);
    }
}
