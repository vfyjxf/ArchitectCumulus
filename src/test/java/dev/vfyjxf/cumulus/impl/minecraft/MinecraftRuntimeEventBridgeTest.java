package dev.vfyjxf.cumulus.impl.minecraft;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;
import dev.vfyjxf.cumulus.impl.runtime.RuntimeCheckEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinecraftRuntimeEventBridgeTest {
    @Test
    void marksChangedBlocksDirtyThroughTracker() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:event_bridge_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:furnace")))
                .build();
        var engine = new RuntimeCheckEngine().register(definition);
        var reader = new FakeLevelTargetReader()
                .block(new BlockPos(2, 64, 2), "minecraft:furnace", Map.of());
        var tracker = new MinecraftRuntimeInstanceTracker(engine);
        var bridge = new MinecraftRuntimeEventBridge(tracker);

        tracker.track(definition.id(), new BlockPos(2, 64, 2), reader, Map.of());
        bridge.markBlockChanged(new BlockPos(2, 64, 2));

        var frame = engine.tick(1, 4);
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
    }

    @Test
    void registersMinecraftBlockMutationListeners() {
        var eventBus = new CapturingEventBus();
        var bridge = new MinecraftRuntimeEventBridge(new MinecraftRuntimeInstanceTracker(new RuntimeCheckEngine()));

        bridge.register(eventBus);

        assertEquals(List.of(
                        BlockEvent.BreakEvent.class,
                        BlockEvent.EntityPlaceEvent.class,
                        BlockEvent.EntityMultiPlaceEvent.class,
                        BlockEvent.FluidPlaceBlockEvent.class),
                eventBus.eventTypes);
    }

    @Test
    void discoversAndTracksExplicitAnchorDefinitionsWhenMatchingAnchorChanges() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:discoverable_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:furnace"))
                        .anchor())
                .build();
        var engine = new RuntimeCheckEngine().register(definition);
        var tracker = new MinecraftRuntimeInstanceTracker(engine);
        var reader = new FakeLevelTargetReader()
                .block(new BlockPos(4, 64, 4), "minecraft:furnace", Map.of());
        var bridge = new MinecraftRuntimeEventBridge(tracker);

        bridge.discovery().register(definition);
        bridge.discoverChangedAnchor(new BlockPos(4, 64, 4), reader, Map.of());

        var frame = engine.tick(1, 4);
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
    }

    @Test
    void discoveryIgnoresChangedBlocksThatDoNotMatchAnchorPredicate() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:discoverable_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:furnace"))
                        .anchor())
                .build();
        var engine = new RuntimeCheckEngine().register(definition);
        var tracker = new MinecraftRuntimeInstanceTracker(engine);
        var reader = new FakeLevelTargetReader()
                .block(new BlockPos(4, 64, 4), "minecraft:stone", Map.of());
        var bridge = new MinecraftRuntimeEventBridge(tracker);

        bridge.discovery().register(definition);
        bridge.discoverChangedAnchor(new BlockPos(4, 64, 4), reader, Map.of());

        var frame = engine.tick(1, 4);
        assertEquals(List.of(), frame.deltas());
    }

    @Test
    void discoveryUsesAnchorPointOffsetWhenTrackingInstanceAnchor() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:offset_discoverable_machine"))
                .point("controller", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:furnace"))
                        .anchor()
                        .capture("controller"))
                .build();
        var engine = new RuntimeCheckEngine().register(definition);
        var tracker = new MinecraftRuntimeInstanceTracker(engine);
        var reader = new FakeLevelTargetReader()
                .block(new BlockPos(5, 64, 4), "minecraft:furnace", Map.of());
        var bridge = new MinecraftRuntimeEventBridge(tracker);

        bridge.discovery().register(definition);
        bridge.discoverChangedAnchor(new BlockPos(5, 64, 4), reader, Map.of());

        var frame = engine.tick(1, 4);
        assertEquals("5,64,4", frame.deltas().getFirst().context().get(dev.vfyjxf.cumulus.api.types.ContextKey.string("controller")).orElse(null));
    }

    @Test
    void discoveryIndexesOnlyDefinitionsWithExplicitAnchorPoints() {
        var discoverable = Structures.define(ResourceLocation.parse("cumulus:discoverable"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block().anchor())
                .build();
        var checkOnly = Structures.define(ResourceLocation.parse("cumulus:check_only"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .build();
        var bridge = new MinecraftRuntimeEventBridge(new MinecraftRuntimeInstanceTracker(new RuntimeCheckEngine()));

        bridge.discovery().register(discoverable);
        bridge.discovery().register(checkOnly);

        assertEquals(List.of(discoverable.id(), checkOnly.id()), bridge.discovery().definitions());
    }

    @Test
    void discoversDefinitionsWithoutExplicitAnchorFromMatchingRequiredPoint() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:inferred_discoverable_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:furnace"))
                        .capture("controller"))
                .build();
        var engine = new RuntimeCheckEngine().register(definition);
        var tracker = new MinecraftRuntimeInstanceTracker(engine);
        var reader = new FakeLevelTargetReader()
                .block(new BlockPos(6, 64, 6), "minecraft:furnace", Map.of());
        var bridge = new MinecraftRuntimeEventBridge(tracker);

        bridge.discovery().register(definition);
        bridge.discoverChangedAnchor(new BlockPos(6, 64, 6), reader, Map.of());

        var frame = engine.tick(1, 4);
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
        assertEquals("6,64,6", frame.deltas().getFirst().context().get(dev.vfyjxf.cumulus.api.types.ContextKey.string("controller")).orElse(null));
    }

    @Test
    void inferredDiscoveryUsesChangedPointOffsetAsInstanceAnchor() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:offset_inferred_discoverable_machine"))
                .point("controller", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:furnace"))
                        .capture("controller"))
                .point("frame", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:stone")))
                .build();
        var engine = new RuntimeCheckEngine().register(definition);
        var tracker = new MinecraftRuntimeInstanceTracker(engine);
        var reader = new FakeLevelTargetReader()
                .block(new BlockPos(5, 64, 4), "minecraft:furnace", Map.of())
                .block(new BlockPos(4, 64, 4), "minecraft:stone", Map.of());
        var bridge = new MinecraftRuntimeEventBridge(tracker);

        bridge.discovery().register(definition);
        bridge.discoverChangedAnchor(new BlockPos(5, 64, 4), reader, Map.of());

        var frame = engine.tick(1, 4);
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
        assertEquals("5,64,4", frame.deltas().getFirst().context().get(dev.vfyjxf.cumulus.api.types.ContextKey.string("controller")).orElse(null));
    }

    @Test
    void discoveryRejectsNullContext() {
        var bridge = new MinecraftRuntimeEventBridge(new MinecraftRuntimeInstanceTracker(new RuntimeCheckEngine()));

        assertThrows(IllegalArgumentException.class,
                () -> bridge.discoverChangedAnchor(BlockPos.ZERO, new FakeLevelTargetReader(), null));
    }

    private static final class FakeLevelTargetReader implements LevelTargetReader {
        private final Map<BlockPos, SnapshotTarget> blocks = new java.util.LinkedHashMap<>();

        FakeLevelTargetReader block(BlockPos pos, String value, Map<String, String> properties) {
            blocks.put(pos, new SnapshotTarget(value, properties));
            return this;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return blocks.containsKey(pos);
        }

        @Override
        public SnapshotTarget block(BlockPos pos) {
            return blocks.get(pos);
        }

        @Override
        public SnapshotTarget blockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public SnapshotTarget fluid(BlockPos pos) {
            return null;
        }

        @Override
        public List<SnapshotTarget> entities(BlockPos min, BlockPos max) {
            return List.of();
        }
    }

    private static final class CapturingEventBus implements IEventBus {
        private final List<Class<? extends Event>> eventTypes = new ArrayList<>();
        private final List<Consumer<? extends Event>> consumers = new ArrayList<>();

        @Override
        public void register(Object target) {
        }

        @Override
        public <T extends Event> void addListener(Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(Class<T> eventType, Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(
                EventPriority priority,
                boolean receiveCanceled,
                Class<T> eventType,
                Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(
                boolean receiveCanceled,
                Class<T> eventType,
                Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public void unregister(Object object) {
        }

        @Override
        public <T extends Event> T post(T event) {
            return event;
        }

        @Override
        public <T extends Event> T post(EventPriority phase, T event) {
            return event;
        }

        @Override
        public void start() {
        }
    }
}
