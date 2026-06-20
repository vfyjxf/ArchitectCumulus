package dev.vfyjxf.cumulus.impl.minecraft;

import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MinecraftCapabilityResolversTest {
    @Test
    void resolvesDefaultEnergyBlockCapabilityWithSideContextAndSnapshotProperties() {
        var storage = new EnergyStorage(400);
        storage.receiveEnergy(175, false);

        var target = MinecraftCapabilityResolvers.defaults().resolve(new MinecraftCapabilityResolvers.CapabilityLookup() {
            @Override
            public <T, C extends @Nullable Object> T get(BlockCapability<T, C> capability, BlockPos pos, C context) {
                assertEquals(Capabilities.EnergyStorage.BLOCK, capability);
                assertEquals(BlockPos.ZERO, pos);
                assertEquals(Direction.NORTH, context);
                return capability.typeClass().cast(storage);
            }
        }, BlockPos.ZERO, "energy", Map.of("side", "north"));

        assertEquals(new SnapshotTarget("energy", Map.of("name", "energy", "capability", "neoforge:energy", "type", EnergyStorage.class.getName(), "side", "north", "energy", "175", "capacity", "400", "can_extract", "true", "can_receive", "true")), target);
    }

    @Test
    void resolvesRegisteredCustomCapabilityByDescriptorName() {
        var capability = BlockCapability.createVoid(ResourceLocation.parse("cumulus:test_flag"), FlagCapability.class);
        var resolvers = MinecraftCapabilityResolvers.empty().register("flag", capability, unused -> Map.of("state", unused.state()));

        var target = resolvers.resolve(new MinecraftCapabilityResolvers.CapabilityLookup() {
            @Override
            public <T, C extends @Nullable Object> T get(BlockCapability<T, C> requested, BlockPos pos, C context) {
                return requested == capability ? requested.typeClass().cast(new FlagCapability("online")) : null;
            }
        }, BlockPos.ZERO, "flag", Map.of());

        assertEquals(new SnapshotTarget("flag", Map.of("name", "flag", "capability", "cumulus:test_flag", "type", FlagCapability.class.getName(), "state", "online")), target);
    }

    @Test
    void leavesUnknownCapabilityMissing() {
        var target = MinecraftCapabilityResolvers.defaults().resolve(new MinecraftCapabilityResolvers.CapabilityLookup() {
            @Override
            public <T, C extends @Nullable Object> T get(BlockCapability<T, C> capability, BlockPos pos, C context) {
                return capability.typeClass().cast(new Object());
            }
        }, BlockPos.ZERO, "missing", Map.of());

        assertNull(target);
    }

    private record FlagCapability(String state) {
    }
}
