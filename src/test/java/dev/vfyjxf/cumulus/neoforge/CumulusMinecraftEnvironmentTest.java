package dev.vfyjxf.cumulus.neoforge;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.Targets;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class CumulusMinecraftEnvironmentTest {
    @BeforeAll
    static void detectMinecraftVersion() {
        SharedConstants.tryDetectVersion();
    }

    @Test
    void minecraftRegistryIsAvailable(MinecraftServer server) {
        assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("minecraft:stone")));
    }

    @Test
    void dslAcceptsMinecraftResourceLocations(MinecraftServer server) {
        var definition = Structures.define(ResourceLocation.parse("cumulus:stone_marker"))
                .point("stone", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:stone")))
                .build();

        assertEquals(ResourceLocation.parse("cumulus:stone_marker"), definition.id());
    }
}
