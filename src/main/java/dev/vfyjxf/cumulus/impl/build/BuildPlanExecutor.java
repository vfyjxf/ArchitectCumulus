package dev.vfyjxf.cumulus.impl.build;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.impl.RuntimePlanEntry;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlan;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class BuildPlanExecutor {
    private static final ResourceLocation block = ResourceLocation.parse("cumulus:block");
    private static final ResourceLocation blockEntity = ResourceLocation.parse("cumulus:block_entity");
    private static final ResourceLocation capability = ResourceLocation.parse("cumulus:capability");
    private static final ResourceLocation entity = ResourceLocation.parse("cumulus:entity");
    private static final ResourceLocation fluid = ResourceLocation.parse("cumulus:fluid");
    private static final ResourceLocation region = ResourceLocation.parse("cumulus:region");

    private BuildPlanExecutor() {
    }

    public static List<BuildAction> actions(StructureRuntimePlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Structure runtime plan must not be null");
        }
        return plan.entries().stream()
                .map(BuildPlanExecutor::action)
                .toList();
    }

    public static BuildPlanResult apply(StructureRuntimePlan plan, BuildTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("Build target must not be null");
        }
        var actions = actions(plan);
        for (var action : actions) {
            if (!supported(action) || !target.supports(action)) {
                return new BuildPlanResult(
                        BuildPlanStatus.unsupportedTarget,
                        actions,
                        unsupportedDiagnostics(action));
            }
        }

        var changed = false;
        for (var action : actions) {
            if (!target.contains(action)) {
                target.apply(action);
                changed = true;
            }
        }
        return new BuildPlanResult(
                changed ? BuildPlanStatus.applied : BuildPlanStatus.noop,
                actions,
                emptyDiagnostics());
    }

    private static BuildAction action(RuntimePlanEntry entry) {
        return new BuildAction(
                entry.id(),
                region.equals(entry.targetType()) ? BuildActionKind.region : BuildActionKind.point,
                entry.targetType(),
                entry.min(),
                entry.max(),
                entry.properties());
    }

    private static boolean supported(BuildAction action) {
        return block.equals(action.targetType())
                || blockEntity.equals(action.targetType())
                || entity.equals(action.targetType())
                || fluid.equals(action.targetType())
                || region.equals(action.targetType())
                || supportedCapability(action);
    }

    private static boolean supportedCapability(BuildAction action) {
        if (!capability.equals(action.targetType())) {
            return false;
        }
        return !action.properties().getOrDefault("block", "").isBlank()
                || !action.properties().getOrDefault("operation", "").isBlank();
    }

    private static DiagnosticGraph unsupportedDiagnostics(BuildAction action) {
        var root = new DiagnosticNode(
                "root",
                Severity.error,
                "Build plan failed",
                "diagnostic.cumulus.build_failed",
                List.of(),
                "runtime/build",
                List.of());
        var node = new DiagnosticNode(
                action.id(),
                Severity.error,
                "Unsupported build target",
                "diagnostic.cumulus.unsupported_build_target",
                List.of(DiagnosticEvidence.relative(
                        action.min(),
                        action.targetType().toString(),
                        "cumulus:build",
                        action.id(),
                        "UNSUPPORTED_BUILD_TARGET",
                        Map.of("to", action.max().asString()))),
                "runtime/build/" + action.id(),
                List.of());
        return new DiagnosticGraph(
                "root",
                List.of(root, node),
                List.of(new DiagnosticEdge("root", action.id(), "caused_by")));
    }

    private static DiagnosticGraph emptyDiagnostics() {
        return new DiagnosticGraph("root", List.of(), List.of());
    }
}
