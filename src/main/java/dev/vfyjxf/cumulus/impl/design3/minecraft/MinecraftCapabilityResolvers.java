package dev.vfyjxf.cumulus.impl.design3.minecraft;

import dev.vfyjxf.cumulus.impl.design3.check.SnapshotTarget;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class MinecraftCapabilityResolvers {
    private final Map<String, BlockResolver<?, ?>> blockResolvers;

    private MinecraftCapabilityResolvers(Map<String, BlockResolver<?, ?>> blockResolvers) {
        this.blockResolvers = Map.copyOf(blockResolvers);
    }

    public static MinecraftCapabilityResolvers empty() {
        return new MinecraftCapabilityResolvers(Map.of());
    }

    public static MinecraftCapabilityResolvers defaults() {
        return MinecraftCapabilityResolvers.empty()
                .register("energy", Capabilities.EnergyStorage.BLOCK, MinecraftCapabilityResolvers::sidedContext,
                        MinecraftCapabilityResolvers::energyProperties)
                .register("item_handler", Capabilities.ItemHandler.BLOCK, MinecraftCapabilityResolvers::sidedContext,
                        MinecraftCapabilityResolvers::itemHandlerProperties)
                .register("fluid_handler", Capabilities.FluidHandler.BLOCK, MinecraftCapabilityResolvers::sidedContext,
                        MinecraftCapabilityResolvers::fluidHandlerProperties);
    }

    public <T> MinecraftCapabilityResolvers register(
            String name,
            BlockCapability<T, @Nullable Void> capability,
            Function<T, Map<String, String>> properties) {
        return register(name, capability, unused -> null, properties);
    }

    public <T, C extends @Nullable Object> MinecraftCapabilityResolvers register(
            String name,
            BlockCapability<T, C> capability,
            Function<Map<String, String>, C> context,
            Function<T, Map<String, String>> properties) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Capability name must not be blank");
        }
        if (capability == null) {
            throw new IllegalArgumentException("Block capability must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Capability context resolver must not be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("Capability property resolver must not be null");
        }
        var next = new LinkedHashMap<>(blockResolvers);
        next.put(name, new BlockResolver<>(capability, context, properties));
        return new MinecraftCapabilityResolvers(next);
    }

    public SnapshotTarget resolve(
            CapabilityLookup lookup,
            BlockPos pos,
            String name,
            Map<String, String> targetProperties) {
        if (lookup == null) {
            throw new IllegalArgumentException("Capability lookup must not be null");
        }
        if (pos == null) {
            throw new IllegalArgumentException("Capability position must not be null");
        }
        if (name == null || name.isBlank()) {
            return null;
        }
        if (targetProperties == null) {
            throw new IllegalArgumentException("Target properties must not be null");
        }
        var resolver = blockResolvers.get(name);
        if (resolver == null) {
            return null;
        }
        return resolver.resolve(lookup, pos, name, targetProperties);
    }

    private static Map<String, String> energyProperties(IEnergyStorage storage) {
        return Map.of(
                "energy", Integer.toString(storage.getEnergyStored()),
                "capacity", Integer.toString(storage.getMaxEnergyStored()),
                "can_extract", Boolean.toString(storage.canExtract()),
                "can_receive", Boolean.toString(storage.canReceive()));
    }

    private static Map<String, String> itemHandlerProperties(IItemHandler handler) {
        var occupied = 0;
        for (var slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                occupied++;
            }
        }
        return Map.of(
                "slots", Integer.toString(handler.getSlots()),
                "occupied_slots", Integer.toString(occupied));
    }

    private static Map<String, String> fluidHandlerProperties(IFluidHandler handler) {
        var filled = 0;
        var capacity = 0;
        var firstFluid = "";
        for (var tank = 0; tank < handler.getTanks(); tank++) {
            var stack = handler.getFluidInTank(tank);
            var amount = stack.getAmount();
            if (!stack.isEmpty() && firstFluid.isBlank()) {
                firstFluid = BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString();
            }
            filled += amount;
            capacity += handler.getTankCapacity(tank);
        }
        var properties = new LinkedHashMap<String, String>();
        properties.put("tanks", Integer.toString(handler.getTanks()));
        properties.put("amount", Integer.toString(filled));
        properties.put("capacity", Integer.toString(capacity));
        if (!firstFluid.isBlank()) {
            properties.put("fluid", firstFluid);
        }
        return properties;
    }

    private static @Nullable Direction sidedContext(Map<String, String> properties) {
        var side = properties.get("side");
        if (side == null || side.isBlank()) {
            return null;
        }
        return Direction.byName(side.toLowerCase(Locale.ROOT));
    }

    @FunctionalInterface
    public interface CapabilityLookup {
        <T, C extends @Nullable Object> T get(BlockCapability<T, C> capability, BlockPos pos, C context);
    }

    private record BlockResolver<T, C extends @Nullable Object>(
            BlockCapability<T, C> capability,
            Function<Map<String, String>, C> context,
            Function<T, Map<String, String>> properties) {
        SnapshotTarget resolve(
                CapabilityLookup lookup,
                BlockPos pos,
                String name,
                Map<String, String> targetProperties) {
            var contextValue = context.apply(targetProperties);
            var value = lookup.get(capability, pos, contextValue);
            if (value == null) {
                return null;
            }
            var snapshotProperties = new LinkedHashMap<String, String>();
            snapshotProperties.put("name", name);
            snapshotProperties.put("capability", capability.name().toString());
            snapshotProperties.put("type", value.getClass().getName());
            var side = targetProperties.get("side");
            if (side != null && !side.isBlank()) {
                snapshotProperties.put("side", side.toLowerCase(Locale.ROOT));
            }
            snapshotProperties.putAll(properties.apply(value));
            return new SnapshotTarget(name, snapshotProperties);
        }
    }
}
