package dev.vfyjxf.cumulus.impl.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftCapabilityBuildWritersTest {
    @Test
    void defaultsRouteEnergyItemAndFluidOperationsToTypedPolicies() {
        var energy = new RecordingTypedWriter("insert_energy");
        var item = new RecordingTypedWriter("insert_item");
        var fluid = new RecordingTypedWriter("insert_fluid");
        var writer = MinecraftCapabilityBuildWriters.defaults(energy, item, fluid);
        var pos = new BlockPos(1, 2, 3);

        assertTrue(writer.canWrite("energy", "insert_energy"));
        assertTrue(writer.canWrite("item_handler", "insert_item"));
        assertTrue(writer.canWrite("fluid_handler", "insert_fluid"));

        writer.mutate(pos, "energy", Map.of("operation", "insert_energy", "amount", "100"));
        writer.mutate(pos, "item_handler", Map.of("operation", "insert_item", "item", "minecraft:iron_ingot"));
        writer.mutate(pos, "fluid_handler", Map.of("operation", "insert_fluid", "fluid", "minecraft:water"));

        assertEquals(List.of(pos), energy.writes());
        assertEquals(List.of(pos), item.writes());
        assertEquals(List.of(pos), fluid.writes());
        assertEquals(Map.of("operation", "insert_energy", "amount", "100"), energy.mutations().get(pos));
    }

    @Test
    void defaultsRejectUnsupportedOperationsWithoutMutatingTypedPolicy() {
        var energy = new RecordingTypedWriter("insert_energy");
        var writer = MinecraftCapabilityBuildWriters.defaults(energy, MinecraftCapabilityBuildWriters.unsupported(), MinecraftCapabilityBuildWriters.unsupported());
        var pos = new BlockPos(1, 2, 3);

        assertFalse(writer.canWrite("energy", "extract_energy"));
        assertFalse(writer.mutate(pos, "energy", Map.of("operation", "extract_energy", "amount", "100")));

        assertEquals(List.of(), energy.writes());
    }

    @Test
    void energyWriterEnsuresMinimumStoredEnergyIdempotently() {
        var storage = new FakeEnergyStorage(1000);
        var writer = MinecraftCapabilityBuildWriters.energy(storage);
        var mutation = Map.of("operation", "ensure_energy", "amount", "250");

        assertTrue(writer.canWrite("ensure_energy"));
        assertFalse(writer.hasMutation(BlockPos.ZERO, mutation));
        assertTrue(writer.mutate(BlockPos.ZERO, mutation));
        assertTrue(writer.hasMutation(BlockPos.ZERO, mutation));
        assertFalse(writer.mutate(BlockPos.ZERO, mutation));

        assertEquals(250, storage.getEnergyStored());
    }

    @Test
    void itemWriterEnsuresItemCountAcrossHandlerSlotsIdempotently() {
        var handler = new FakeItemHandler(2);
        var writer = MinecraftCapabilityBuildWriters.item(handler);
        var mutation = Map.of("operation", "ensure_item", "item", "minecraft:coal", "count", "12");

        assertTrue(writer.canWrite("ensure_item"));
        assertFalse(writer.hasMutation(BlockPos.ZERO, mutation));
        assertTrue(writer.mutate(BlockPos.ZERO, mutation));
        assertTrue(writer.hasMutation(BlockPos.ZERO, mutation));
        assertFalse(writer.mutate(BlockPos.ZERO, mutation));

        assertEquals(12, handler.count(Items.COAL));
    }

    @Test
    void fluidWriterEnsuresFluidAmountAcrossTanksIdempotently() {
        var handler = new FakeFluidHandler(1000);
        var writer = MinecraftCapabilityBuildWriters.fluid(handler);
        var mutation = Map.of("operation", "ensure_fluid", "fluid", "minecraft:water", "amount", "400");

        assertTrue(writer.canWrite("ensure_fluid"));
        assertFalse(writer.hasMutation(BlockPos.ZERO, mutation));
        assertTrue(writer.mutate(BlockPos.ZERO, mutation));
        assertTrue(writer.hasMutation(BlockPos.ZERO, mutation));
        assertFalse(writer.mutate(BlockPos.ZERO, mutation));

        assertEquals(400, handler.amount(Fluids.WATER));
    }

    private static final class RecordingTypedWriter implements TypedCapabilityBuildWriter {
        private final String supportedOperation;
        private final Map<BlockPos, Map<String, String>> mutations = new LinkedHashMap<>();
        private final List<BlockPos> writes = new java.util.ArrayList<>();

        RecordingTypedWriter(String supportedOperation) {
            this.supportedOperation = supportedOperation;
        }

        List<BlockPos> writes() {
            return List.copyOf(writes);
        }

        Map<BlockPos, Map<String, String>> mutations() {
            var values = new LinkedHashMap<BlockPos, Map<String, String>>();
            mutations.forEach((pos, mutation) -> values.put(pos, Map.copyOf(mutation)));
            return values;
        }

        @Override
        public boolean canWrite(String operation) {
            return supportedOperation.equals(operation);
        }

        @Override
        public boolean hasMutation(BlockPos pos, Map<String, String> mutation) {
            return mutation.equals(mutations.get(pos));
        }

        @Override
        public boolean mutate(BlockPos pos, Map<String, String> mutation) {
            if (!canWrite(mutation.getOrDefault("operation", "")) || hasMutation(pos, mutation)) {
                return false;
            }
            mutations.put(pos, Map.copyOf(mutation));
            writes.add(pos);
            return true;
        }
    }

    private static final class FakeEnergyStorage implements IEnergyStorage {
        private final int capacity;
        private int energy;

        FakeEnergyStorage(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            var received = Math.min(maxReceive, capacity - energy);
            if (!simulate) {
                energy += received;
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            var extracted = Math.min(maxExtract, energy);
            if (!simulate) {
                energy -= extracted;
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    private static final class FakeItemHandler implements IItemHandler {
        private final ItemStack[] stacks;

        FakeItemHandler(int slots) {
            this.stacks = new ItemStack[slots];
            java.util.Arrays.fill(stacks, ItemStack.EMPTY);
        }

        int count(net.minecraft.world.item.Item item) {
            var count = 0;
            for (var stack : stacks) {
                if (!stack.isEmpty() && stack.is(item)) {
                    count += stack.getCount();
                }
            }
            return count;
        }

        @Override
        public int getSlots() {
            return stacks.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stacks[slot];
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var existing = stacks[slot];
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                return stack;
            }
            var existingCount = existing.isEmpty() ? 0 : existing.getCount();
            var accepted = Math.min(stack.getCount(), getSlotLimit(slot) - existingCount);
            if (!simulate && accepted > 0) {
                stacks[slot] = stack.copyWithCount(existingCount + accepted);
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            var existing = stacks[slot];
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            var extracted = Math.min(amount, existing.getCount());
            if (!simulate) {
                stacks[slot] = existing.copyWithCount(existing.getCount() - extracted);
            }
            return existing.copyWithCount(extracted);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }

    private static final class FakeFluidHandler implements IFluidHandler {
        private final int capacity;
        private FluidStack stack = FluidStack.EMPTY;

        FakeFluidHandler(int capacity) {
            this.capacity = capacity;
        }

        int amount(net.minecraft.world.level.material.Fluid fluid) {
            return !stack.isEmpty() && stack.is(fluid) ? stack.getAmount() : 0;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return stack;
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || (!stack.isEmpty() && !FluidStack.isSameFluidSameComponents(stack, resource))) {
                return 0;
            }
            var existingAmount = stack.isEmpty() ? 0 : stack.getAmount();
            var filled = Math.min(resource.getAmount(), capacity - existingAmount);
            if (action.execute() && filled > 0) {
                stack = resource.copyWithAmount(existingAmount + filled);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (stack.isEmpty() || resource.isEmpty() || !FluidStack.isSameFluidSameComponents(stack, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }
            var drained = Math.min(maxDrain, stack.getAmount());
            var result = stack.copyWithAmount(drained);
            if (action.execute()) {
                stack = stack.copyWithAmount(stack.getAmount() - drained);
            }
            return result;
        }
    }
}
