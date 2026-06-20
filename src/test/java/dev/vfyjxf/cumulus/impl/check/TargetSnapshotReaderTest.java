package dev.vfyjxf.cumulus.impl.check;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetSnapshotReaderTest {
    @Test
    void readsDefinitionTargetsFromWorldLikeSourceAndFeedsCheckExecutor() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:adapter_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .point("inventory", RelativeBlockPos.of(1, 0, 0), Targets.blockEntity()
                        .where(PredicateDescriptors.id("cumulus:item_handler")))
                .point("worker", RelativeBlockPos.of(0, 1, 0), Targets.entity()
                        .where(PredicateDescriptors.id("cumulus:worker")))
                .point("coolant", RelativeBlockPos.of(0, 0, 1), Targets.fluid()
                        .where(PredicateDescriptors.id("minecraft:water")))
                .point("energy", RelativeBlockPos.of(1, 1, 0), Targets.capability("energy")
                        .where(PredicateDescriptors.property("side", "north")))
                .point("facing", RelativeBlockPos.of(0, 0, 0), Targets.context("facing")
                        .where(PredicateDescriptors.id("north")))
                .component(Components.region("door", RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 2, 0))
                        .where(PredicateDescriptors.id("cumulus:door_frame")))
                .build();
        var source = new FakeTargetSnapshotSource()
                .put(ResourceLocation.parse("cumulus:block"), RelativeBlockPos.of(0, 0, 0), RelativeBlockPos.of(0, 0, 0),
                        "cumulus:controller")
                .put(ResourceLocation.parse("cumulus:block_entity"), RelativeBlockPos.of(1, 0, 0), RelativeBlockPos.of(1, 0, 0),
                        "cumulus:item_handler")
                .put(ResourceLocation.parse("cumulus:entity"), RelativeBlockPos.of(0, 1, 0), RelativeBlockPos.of(0, 1, 0),
                        "cumulus:worker")
                .put(ResourceLocation.parse("cumulus:fluid"), RelativeBlockPos.of(0, 0, 1), RelativeBlockPos.of(0, 0, 1),
                        "minecraft:water")
                .put(ResourceLocation.parse("cumulus:capability"), RelativeBlockPos.of(1, 1, 0), RelativeBlockPos.of(1, 1, 0),
                        "energy", Map.of("side", "north"))
                .put(ResourceLocation.parse("cumulus:region"), RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 2, 0),
                        "cumulus:door_frame")
                .context("facing", "north");

        var snapshot = TargetSnapshotReader.fromDefinition(definition, source);
        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 64, 0), snapshot);

        assertEquals(InstanceStatus.valid, result.status());
    }

    @Test
    void appliesDeclaredTransformWhenReadingSnapshotCandidate() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:rotated_adapter_machine"))
                .transforms(Transforms.identity(), Transforms.rotateY90())
                .point("controller", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();
        var source = new FakeTargetSnapshotSource()
                .put(ResourceLocation.parse("cumulus:block"), RelativeBlockPos.of(0, 0, 1), RelativeBlockPos.of(0, 0, 1),
                        "cumulus:controller");

        var snapshot = TargetSnapshotReader.fromDefinition(definition, source);
        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);

        assertEquals(InstanceStatus.valid, result.status());
    }

    @Test
    void compiledCheckPlanFeedsSnapshotReaderAndExecutor() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:compiled_adapter_machine"))
                .transforms(Transforms.identity(), Transforms.rotateY90())
                .point("controller", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();
        var plan = StructureRuntimePlanCompiler.compileCheckPlan(definition);
        var source = new FakeTargetSnapshotSource()
                .put(ResourceLocation.parse("cumulus:block"), RelativeBlockPos.of(0, 0, 1), RelativeBlockPos.of(0, 0, 1),
                        "cumulus:controller");

        var snapshot = TargetSnapshotReader.fromPlan(plan, source);
        var result = StructureCheckExecutor.check(plan, new BlockPos(0, 0, 0), snapshot);

        assertEquals(InstanceStatus.valid, result.status());
    }

    @Test
    void missingRequiredSourceDataLeavesSnapshotIncompleteWithoutForcingData() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:missing_adapter_machine"))
                .point("controller", RelativeBlockPos.of(32, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();

        var snapshot = TargetSnapshotReader.fromDefinition(definition, new FakeTargetSnapshotSource());
        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);

        assertEquals(InstanceStatus.incomplete, result.status());
        assertEquals(1, result.requests().size());
    }

    private static final class FakeTargetSnapshotSource implements TargetSnapshotSource {
        private final Map<Key, SnapshotTarget> targets = new LinkedHashMap<>();
        private final Map<String, String> context = new LinkedHashMap<>();

        FakeTargetSnapshotSource put(ResourceLocation type, RelativeBlockPos min, RelativeBlockPos max, String value) {
            return put(type, min, max, value, Map.of());
        }

        FakeTargetSnapshotSource put(
                ResourceLocation type,
                RelativeBlockPos min,
                RelativeBlockPos max,
                String value,
                Map<String, String> properties) {
            targets.put(new Key(type, min, max), new SnapshotTarget(value, properties));
            return this;
        }

        FakeTargetSnapshotSource context(String key, String value) {
            context.put(key, value);
            return this;
        }

        @Override
        public Optional<SnapshotTarget> target(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max) {
            return Optional.ofNullable(targets.get(new Key(targetType, min, max)));
        }

        @Override
        public Optional<String> context(String key) {
            return Optional.ofNullable(context.get(key));
        }

        private record Key(ResourceLocation type, RelativeBlockPos min, RelativeBlockPos max) {
        }
    }
}
