package dev.vfyjxf.cumulus.api.target;

import dev.vfyjxf.cumulus.api.target.types.UnknownTargetType;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the open {@link TargetType} / {@link TargetTypes} registry and the
 * round-trip behavior of typed {@link TargetDescriptor}.
 */
class CustomTargetTypeTest {

    @Test
    void builtInTargetTypesArePreRegistered() {
        assertEquals(7, TargetTypes.size());
        assertTrue(TargetTypes.isRegistered(ResourceLocation.parse("cumulus:block")));
        assertTrue(TargetTypes.isRegistered(ResourceLocation.parse("cumulus:block_entity")));
        assertTrue(TargetTypes.isRegistered(ResourceLocation.parse("cumulus:entity")));
        assertTrue(TargetTypes.isRegistered(ResourceLocation.parse("cumulus:fluid")));
        assertTrue(TargetTypes.isRegistered(ResourceLocation.parse("cumulus:capability")));
        assertTrue(TargetTypes.isRegistered(ResourceLocation.parse("cumulus:context")));
        assertTrue(TargetTypes.isRegistered(ResourceLocation.parse("cumulus:region")));
    }

    @Test
    void unknownTargetTypeLookupReturnsEmptyOptional() {
        var unknown = TargetTypes.maybeFromId(ResourceLocation.parse("mymod:custom_target"));
        assertTrue(unknown.isEmpty());
    }

    @Test
    void unknownTargetTypePreservesOriginalId() {
        var id = ResourceLocation.parse("mymod:custom_target");
        var wrapped = UnknownTargetType.of(id);
        assertEquals(id, wrapped.id());
        assertEquals(id, wrapped.id());
    }

    @Test
    void targetDescriptorTypeIdExposesUnderlyingTypeId() {
        var desc = new TargetDescriptor(
                        ResourceLocation.parse("cumulus:capability"),
                        Map.of("name", "cumulus:energy"));
        assertEquals(ResourceLocation.parse("cumulus:capability"), desc.typeId());
    }

    @Test
    void targetDescriptorPropertiesExposesStringMapView() {
        var desc = new TargetDescriptor(
                        ResourceLocation.parse("cumulus:capability"),
                        Map.of("name", "cumulus:energy"));
        var props = desc.properties();
        assertEquals("cumulus:energy", props.get("name"));
    }

    @Test
    void targetDescriptorRejectsNullTypeId() {
        assertThrows(IllegalArgumentException.class, () -> new TargetDescriptor(
                        null,
                        Map.of()));
    }

    @Test
    void unknownTargetTypePropsPreservesRawMap() {
        var raw = Map.of("foo", "bar", "baz", "qux");
        var props = new UnknownTargetType.Props(raw);
        assertEquals(raw, props.raw());
    }
}
