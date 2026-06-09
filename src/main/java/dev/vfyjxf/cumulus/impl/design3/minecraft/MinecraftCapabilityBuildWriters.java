package dev.vfyjxf.cumulus.impl.design3.minecraft;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public final class MinecraftCapabilityBuildWriters {
    private static final TypedCapabilityBuildWriter UNSUPPORTED = new TypedCapabilityBuildWriter() {
        @Override
        public boolean canWrite(String operation) {
            return false;
        }

        @Override
        public boolean hasMutation(BlockPos pos, Map<String, String> mutation) {
            return false;
        }

        @Override
        public boolean mutate(BlockPos pos, Map<String, String> mutation) {
            return false;
        }
    };

    private MinecraftCapabilityBuildWriters() {
    }

    public static CapabilityBuildWriter defaults(
            TypedCapabilityBuildWriter energy,
            TypedCapabilityBuildWriter item,
            TypedCapabilityBuildWriter fluid) {
        if (energy == null) {
            throw new IllegalArgumentException("Energy capability build writer must not be null");
        }
        if (item == null) {
            throw new IllegalArgumentException("Item capability build writer must not be null");
        }
        if (fluid == null) {
            throw new IllegalArgumentException("Fluid capability build writer must not be null");
        }
        return new RoutingCapabilityBuildWriter(Map.of(
                "energy", energy,
                "item_handler", item,
                "fluid_handler", fluid));
    }

    public static TypedCapabilityBuildWriter unsupported() {
        return UNSUPPORTED;
    }

    public static TypedCapabilityBuildWriter energy(IEnergyStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Energy storage must not be null");
        }
        return new EnergyCapabilityBuildWriter(storage);
    }

    public static TypedCapabilityBuildWriter item(IItemHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Item handler must not be null");
        }
        return new ItemCapabilityBuildWriter(handler);
    }

    public static TypedCapabilityBuildWriter fluid(IFluidHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Fluid handler must not be null");
        }
        return new FluidCapabilityBuildWriter(handler);
    }

    private record RoutingCapabilityBuildWriter(
            Map<String, TypedCapabilityBuildWriter> writers) implements CapabilityBuildWriter {
        private RoutingCapabilityBuildWriter {
            writers = Map.copyOf(writers);
        }

        @Override
        public boolean canWrite(String capabilityName, String operation) {
            var writer = writers.get(capabilityName);
            return writer != null && writer.canWrite(operation);
        }

        @Override
        public boolean hasMutation(BlockPos pos, String capabilityName, Map<String, String> mutation) {
            var writer = writers.get(capabilityName);
            return writer != null && writer.hasMutation(pos, mutation);
        }

        @Override
        public boolean mutate(BlockPos pos, String capabilityName, Map<String, String> mutation) {
            var writer = writers.get(capabilityName);
            return writer != null && writer.canWrite(mutation.getOrDefault("operation", ""))
                    && writer.mutate(pos, mutation);
        }
    }

    private record EnergyCapabilityBuildWriter(IEnergyStorage storage) implements TypedCapabilityBuildWriter {
        private static final String ENSURE_ENERGY = "ensure_energy";

        @Override
        public boolean canWrite(String operation) {
            return ENSURE_ENERGY.equals(operation) && storage.canReceive();
        }

        @Override
        public boolean hasMutation(BlockPos pos, Map<String, String> mutation) {
            return storage.getEnergyStored() >= amount(mutation, "amount");
        }

        @Override
        public boolean mutate(BlockPos pos, Map<String, String> mutation) {
            if (!canWrite(mutation.getOrDefault("operation", "")) || hasMutation(pos, mutation)) {
                return false;
            }
            var missing = amount(mutation, "amount") - storage.getEnergyStored();
            return storage.receiveEnergy(missing, false) > 0;
        }
    }

    private record ItemCapabilityBuildWriter(IItemHandler handler) implements TypedCapabilityBuildWriter {
        private static final String ENSURE_ITEM = "ensure_item";

        @Override
        public boolean canWrite(String operation) {
            return ENSURE_ITEM.equals(operation);
        }

        @Override
        public boolean hasMutation(BlockPos pos, Map<String, String> mutation) {
            var stack = itemStack(mutation);
            return !stack.isEmpty() && count(stack) >= stack.getCount();
        }

        @Override
        public boolean mutate(BlockPos pos, Map<String, String> mutation) {
            if (!canWrite(mutation.getOrDefault("operation", "")) || hasMutation(pos, mutation)) {
                return false;
            }
            var target = itemStack(mutation);
            if (target.isEmpty()) {
                return false;
            }
            var remaining = target.getCount() - count(target);
            var changed = false;
            for (var slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                var inserted = target.copyWithCount(remaining);
                var leftover = handler.insertItem(slot, inserted, false);
                var accepted = inserted.getCount() - leftover.getCount();
                if (accepted > 0) {
                    changed = true;
                    remaining -= accepted;
                }
            }
            return changed;
        }

        private int count(ItemStack target) {
            var count = 0;
            for (var slot = 0; slot < handler.getSlots(); slot++) {
                var stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, target)) {
                    count += stack.getCount();
                }
            }
            return count;
        }
    }

    private record FluidCapabilityBuildWriter(IFluidHandler handler) implements TypedCapabilityBuildWriter {
        private static final String ENSURE_FLUID = "ensure_fluid";

        @Override
        public boolean canWrite(String operation) {
            return ENSURE_FLUID.equals(operation);
        }

        @Override
        public boolean hasMutation(BlockPos pos, Map<String, String> mutation) {
            var stack = fluidStack(mutation);
            return !stack.isEmpty() && amount(stack) >= stack.getAmount();
        }

        @Override
        public boolean mutate(BlockPos pos, Map<String, String> mutation) {
            if (!canWrite(mutation.getOrDefault("operation", "")) || hasMutation(pos, mutation)) {
                return false;
            }
            var target = fluidStack(mutation);
            if (target.isEmpty()) {
                return false;
            }
            var missing = target.getAmount() - amount(target);
            return handler.fill(target.copyWithAmount(missing), IFluidHandler.FluidAction.EXECUTE) > 0;
        }

        private int amount(FluidStack target) {
            var amount = 0;
            for (var tank = 0; tank < handler.getTanks(); tank++) {
                var stack = handler.getFluidInTank(tank);
                if (!stack.isEmpty() && FluidStack.isSameFluidSameComponents(stack, target)) {
                    amount += stack.getAmount();
                }
            }
            return amount;
        }
    }

    private static int amount(Map<String, String> mutation, String key) {
        return Integer.parseInt(mutation.getOrDefault(key, "0"));
    }

    private static ItemStack itemStack(Map<String, String> mutation) {
        var itemId = mutation.get("item");
        if (itemId == null || itemId.isBlank()) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, amount(mutation, "count"));
    }

    private static FluidStack fluidStack(Map<String, String> mutation) {
        var fluidId = mutation.get("fluid");
        if (fluidId == null || fluidId.isBlank()) {
            return FluidStack.EMPTY;
        }
        var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId));
        if (fluid == null) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amount(mutation, "amount"));
    }
}
