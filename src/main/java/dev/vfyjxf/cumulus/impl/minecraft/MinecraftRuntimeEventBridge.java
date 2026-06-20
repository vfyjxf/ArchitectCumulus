package dev.vfyjxf.cumulus.impl.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Map;

public final class MinecraftRuntimeEventBridge {
    private final MinecraftRuntimeInstanceTracker tracker;
    private final MinecraftRuntimeDiscovery discovery;

    public MinecraftRuntimeEventBridge(MinecraftRuntimeInstanceTracker tracker) {
        if (tracker == null) {
            throw new IllegalArgumentException("Minecraft runtime instance tracker must not be null");
        }
        this.tracker = tracker;
        this.discovery = new MinecraftRuntimeDiscovery(tracker);
    }

    public void register(IEventBus eventBus) {
        if (eventBus == null) {
            throw new IllegalArgumentException("Event bus must not be null");
        }
        eventBus.addListener(BlockEvent.BreakEvent.class, this::onBlockBreak);
        eventBus.addListener(BlockEvent.EntityPlaceEvent.class, this::onBlockPlace);
        eventBus.addListener(BlockEvent.EntityMultiPlaceEvent.class, this::onBlockMultiPlace);
        eventBus.addListener(BlockEvent.FluidPlaceBlockEvent.class, this::onFluidPlaceBlock);
    }

    public void markBlockChanged(BlockPos pos) {
        if (pos == null) {
            throw new IllegalArgumentException("Changed block position must not be null");
        }
        tracker.markChanged(pos);
    }

    public MinecraftRuntimeDiscovery discovery() {
        return discovery;
    }

    public void discoverChangedAnchor(BlockPos pos, LevelAccessor level, Map<String, String> context) {
        discovery.discoverChangedAnchor(pos, level, context);
    }

    public void discoverChangedAnchor(BlockPos pos, LevelTargetReader reader, Map<String, String> context) {
        discovery.discoverChangedAnchor(pos, reader, context);
    }

    void onBlockBreak(BlockEvent.BreakEvent event) {
        markBlockChanged(event.getPos());
        discovery.discoverChangedAnchor(event.getPos(), event.getLevel(), Map.of());
    }

    void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        markBlockChanged(event.getPos());
        discovery.discoverChangedAnchor(event.getPos(), event.getLevel(), Map.of());
    }

    void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        for (var snapshot : event.getReplacedBlockSnapshots()) {
            markBlockChanged(snapshot.getPos());
            discovery.discoverChangedAnchor(snapshot.getPos(), event.getLevel(), Map.of());
        }
    }

    void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        markBlockChanged(event.getPos());
        discovery.discoverChangedAnchor(event.getPos(), event.getLevel(), Map.of());
    }
}
