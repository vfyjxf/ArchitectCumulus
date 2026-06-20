package dev.vfyjxf.cumulus.api.target;

import dev.vfyjxf.cumulus.api.target.types.BlockTargetType;
import dev.vfyjxf.cumulus.api.target.types.ContextTargetType;
import dev.vfyjxf.cumulus.api.target.types.RegionTargetType;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedTargetTypeTest {

    @Test
    void descriptorTypeLookupResolves() {
        var desc = new TargetDescriptor(BlockTargetType.id, Map.of());
        var type = desc.type();
        assertTrue(type.isPresent());
        assertEquals(BlockTargetType.instance, type.orElseThrow());
    }

    @Test
    void descriptorIsTypeChecksCorrectly() {
        var desc = new TargetDescriptor(RegionTargetType.id, Map.of());
        assertTrue(desc.isType(RegionTargetType.instance));
    }

    @Test
    void targetTypesRegistryResolvesAllBuiltins() {
        assertTrue(TargetTypes.maybeFromId(BlockTargetType.id).isPresent());
        assertTrue(TargetTypes.maybeFromId(ContextTargetType.instance.id()).isPresent());
        assertTrue(TargetTypes.maybeFromId(RegionTargetType.instance.id()).isPresent());
    }

    @Test
    void targetTypesIsRegisteredWorks() {
        assertTrue(TargetTypes.isRegistered(BlockTargetType.id));
    }

    @Test
    void unknownTypeReturnsEmpty() {
        assertTrue(TargetTypes.maybeFromId(ResourceLocation.parse("mymod:unknown")).isEmpty());
    }
}
