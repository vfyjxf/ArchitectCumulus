package dev.vfyjxf.cumulus.impl.design3;

import dev.vfyjxf.cumulus.api.component.ComponentDescriptor;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.diagnostic.FixHint;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.visualization.VisualizationModel;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record StructureRuntimePlan(
        ResourceLocation definitionId,
        List<RuntimePlanEntry> entries,
        List<ComponentDescriptor> substructures,
        List<FixHint> buildHints,
        VisualizationModel visualization) {
    public StructureRuntimePlan {
        if (definitionId == null) {
            throw new IllegalArgumentException("Runtime plan definition id must not be null");
        }
        entries = List.copyOf(entries);
        substructures = List.copyOf(substructures);
        buildHints = List.copyOf(buildHints);
        if (visualization == null) {
            throw new IllegalArgumentException("Runtime plan visualization must not be null");
        }
    }

    public Set<SectionKey> footprintSections(BlockPos anchor) {
        if (anchor == null) {
            throw new IllegalArgumentException("Runtime plan anchor must not be null");
        }

        var sections = new LinkedHashSet<SectionKey>();
        for (var entry : entries) {
            var minSection = SectionKey.fromBlock(anchor.offset(entry.min().x(), entry.min().y(), entry.min().z()));
            var maxSection = SectionKey.fromBlock(anchor.offset(entry.max().x(), entry.max().y(), entry.max().z()));
            for (var x = minSection.x(); x <= maxSection.x(); x++) {
                for (var y = minSection.y(); y <= maxSection.y(); y++) {
                    for (var z = minSection.z(); z <= maxSection.z(); z++) {
                        sections.add(new SectionKey(x, y, z));
                    }
                }
            }
        }
        return Set.copyOf(sections);
    }
}
