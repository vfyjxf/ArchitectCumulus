package dev.vfyjxf.cumulus.impl.design3.minecraft;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.impl.design3.check.PredicateEvaluator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;

public final class MinecraftRuntimeDiscovery {
    private final MinecraftRuntimeInstanceTracker tracker;
    private final Map<ResourceLocation, List<DiscoveryCandidate>> candidatesByDefinition = new LinkedHashMap<>();

    public MinecraftRuntimeDiscovery(MinecraftRuntimeInstanceTracker tracker) {
        if (tracker == null) {
            throw new IllegalArgumentException("Minecraft runtime instance tracker must not be null");
        }
        this.tracker = tracker;
    }

    public void register(StructureDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }
        var candidates = discoveryCandidates(definition);
        if (!candidates.isEmpty()) {
            candidatesByDefinition.put(definition.id(), candidates);
        }
    }

    public void discoverChangedAnchor(BlockPos changedBlock, LevelAccessor level, Map<String, String> context) {
        if (level == null) {
            throw new IllegalArgumentException("Level must not be null");
        }
        discoverChangedAnchor(changedBlock, new MinecraftTargetSnapshotSource.LevelAccessorTargetReader(level), context);
    }

    public void discoverChangedAnchor(BlockPos changedBlock, LevelTargetReader reader, Map<String, String> context) {
        if (changedBlock == null) {
            throw new IllegalArgumentException("Changed block position must not be null");
        }
        if (reader == null) {
            throw new IllegalArgumentException("Level target reader must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Discovery context must not be null");
        }
        for (var candidates : candidatesByDefinition.values()) {
            for (var candidate : candidates) {
                var instanceAnchor = changedBlock.offset(
                        -candidate.offset().x(),
                        -candidate.offset().y(),
                        -candidate.offset().z());
                if (!matchesCandidate(candidate.point(), changedBlock, reader)) {
                    continue;
                }
                tracker.track(candidate.definitionId(), instanceAnchor, reader, context);
                tracker.markChanged(changedBlock);
                break;
            }
        }
    }

    private static List<DiscoveryCandidate> discoveryCandidates(StructureDefinition definition) {
        var explicit = definition.points().stream()
                .filter(point -> Boolean.parseBoolean(point.target().properties().getOrDefault("anchor", "false")))
                .map(point -> new DiscoveryCandidate(definition.id(), point.location(), point))
                .toList();
        if (!explicit.isEmpty()) {
            return explicit;
        }
        var inferred = new ArrayList<DiscoveryCandidate>();
        for (var point : definition.points()) {
            if (point.requirement() == PointRequirement.REQUIRED) {
                inferred.add(new DiscoveryCandidate(definition.id(), point.location(), point));
            }
        }
        return List.copyOf(inferred);
    }

    private static boolean matchesCandidate(StructurePointDefinition point, BlockPos changedBlock, LevelTargetReader reader) {
        if (!reader.isLoaded(changedBlock)) {
            return false;
        }
        var target = new MinecraftTargetSnapshotSource(BlockPos.ZERO, reader, Map.of())
                .target(point.target().typeId(), RelativeBlockPos.of(
                                changedBlock.getX(),
                                changedBlock.getY(),
                                changedBlock.getZ()),
                        RelativeBlockPos.of(
                                changedBlock.getX(),
                                changedBlock.getY(),
                                changedBlock.getZ()))
                .orElse(null);
        if (target == null) {
            return false;
        }
        for (var predicate : point.predicates()) {
            if (PredicateEvaluator.evaluate(predicate, target) != PredicateEvaluator.Evaluation.MATCH) {
                return false;
            }
        }
        return true;
    }

    public List<ResourceLocation> definitions() {
        return List.copyOf(candidatesByDefinition.keySet());
    }

    private record DiscoveryCandidate(
            ResourceLocation definitionId,
            RelativeBlockPos offset,
            StructurePointDefinition point) {
    }
}
