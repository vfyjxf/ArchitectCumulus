package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;
import java.util.Comparator;
import net.minecraft.resources.ResourceLocation;

public final class TargetSnapshotReader {
    private static final ResourceLocation block = ResourceLocation.parse("cumulus:block");
    private static final ResourceLocation blockEntity = ResourceLocation.parse("cumulus:block_entity");
    private static final ResourceLocation entity = ResourceLocation.parse("cumulus:entity");
    private static final ResourceLocation fluid = ResourceLocation.parse("cumulus:fluid");
    private static final ResourceLocation capability = ResourceLocation.parse("cumulus:capability");
    private static final ResourceLocation region = ResourceLocation.parse("cumulus:region");
    private static final ResourceLocation context = ResourceLocation.parse("cumulus:context");

    private TargetSnapshotReader() {
    }

    public static InMemoryTargetSnapshot fromDefinition(StructureDefinition definition, TargetSnapshotSource source) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Target snapshot source must not be null");
        }

        return fromPlan(StructureRuntimePlanCompiler.compileCheckPlan(definition), source);
    }

    public static InMemoryTargetSnapshot fromPlan(StructureCheckPlan plan, TargetSnapshotSource source) {
        if (plan == null) {
            throw new IllegalArgumentException("Structure check plan must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Target snapshot source must not be null");
        }

        Candidate best = null;
        for (var checkCandidate : plan.candidates()) {
            var candidate = readCandidate(checkCandidate, source);
            if (best == null || candidate.compareTo(best) > 0) {
                best = candidate;
            }
            if (candidate.complete()) {
                return candidate.snapshot();
            }
        }
        return best == null ? InMemoryTargetSnapshot.empty() : best.snapshot();
    }

    private static Candidate readCandidate(
            CheckPlanCandidate checkCandidate,
            TargetSnapshotSource source) {
        var builder = InMemoryTargetSnapshot.builder();
        var presentRequired = 0;
        var missingRequired = 0;
        for (var entry : checkCandidate.entries()) {
            var point = entry.point();
            if (context.equals(point.target().typeId())) {
                var key = point.target().properties().get("key");
                var value = source.context(key);
                if (value.isPresent()) {
                    builder.context(key, value.orElseThrow());
                    if (point.requirement() == PointRequirement.required) {
                        presentRequired++;
                    }
                } else if (point.requirement() == PointRequirement.required) {
                    missingRequired++;
                }
                continue;
            }

            var target = source.target(
                    point.target().typeId(),
                    entry.min(),
                    entry.max(),
                    point.target().properties());
            if (target.isPresent()) {
                put(builder, point.target().typeId(), entry, target.orElseThrow());
                if (point.requirement() == PointRequirement.required) {
                    presentRequired++;
                }
            } else if (point.requirement() == PointRequirement.required) {
                missingRequired++;
            }
        }
        return new Candidate(builder.build(), presentRequired, missingRequired);
    }

    private static void put(
            InMemoryTargetSnapshot.Builder builder,
            ResourceLocation targetType,
            CheckPlanEntry entry,
            SnapshotTarget target) {
        if (block.equals(targetType)) {
            builder.block(entry.min(), target.value(), target.properties());
            return;
        }
        if (blockEntity.equals(targetType)) {
            builder.blockEntity(entry.min(), target.value(), target.properties());
            return;
        }
        if (entity.equals(targetType)) {
            builder.entity(entry.min(), target.value(), target.properties());
            return;
        }
        if (fluid.equals(targetType)) {
            builder.fluid(entry.min(), target.value(), target.properties());
            return;
        }
        if (capability.equals(targetType)) {
            builder.capability(entry.min(), target.value(), target.properties());
            return;
        }
        if (region.equals(targetType)) {
            builder.region(entry.min(), entry.max(), target.value(), target.properties());
        }
    }

    private record Candidate(
            InMemoryTargetSnapshot snapshot,
            int presentRequired,
            int missingRequired) implements Comparable<Candidate> {
        private boolean complete() {
            return missingRequired == 0;
        }

        @Override
        public int compareTo(Candidate other) {
            return Comparator
                    .comparingInt(Candidate::presentRequired)
                    .thenComparing(candidate -> -candidate.missingRequired())
                    .compare(this, other);
        }
    }
}
