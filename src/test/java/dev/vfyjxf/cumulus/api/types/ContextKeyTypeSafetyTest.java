package dev.vfyjxf.cumulus.api.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Verifies the type-safety contract of {@link ContextKey} and
 * {@link RuntimeStructureContext}: typed reads return the stored value, missing
 * keys return empty, and keys are distinct by value type.
 */
class ContextKeyTypeSafetyTest {

    private static final ContextKey<Direction> facing = ContextKey.direction("facing");
    private static final ContextKey<Integer> tier = ContextKey.integer("tier");
    private static final ContextKey<BlockPos> controller = ContextKey.blockPos("controller");
    private static final ContextKey<ResourceLocation> controllerId =
            ContextKey.resourceLocation("controller_id");
    private static final ContextKey<String> label = ContextKey.string("label");

    private static StructureInstanceKey testKey() {
        return new StructureInstanceKey(ResourceLocation.parse("cumulus:test"), new BlockPos(0, 64, 0));
    }

    @Test
    void typedPutAndReadRoundTripsValue() {
        var ctx = RuntimeStructureContext.builder(testKey())
                .put(facing, Direction.NORTH)
                .put(tier, 3)
                .put(controller, new BlockPos(10, 65, 10))
                .put(controllerId, ResourceLocation.parse("cumulus:controller"))
                .put(label, "main_machine")
                .build();

        assertEquals(Direction.NORTH, ctx.get(facing).orElseThrow());
        assertEquals(3, ctx.get(tier).orElseThrow());
        assertEquals(new BlockPos(10, 65, 10), ctx.get(controller).orElseThrow());
        assertEquals(ResourceLocation.parse("cumulus:controller"), ctx.get(controllerId).orElseThrow());
        assertEquals("main_machine", ctx.get(label).orElseThrow());
    }

    @Test
    void missingKeyReturnsEmptyOptional() {
        var ctx = RuntimeStructureContext.builder(testKey()).build();
        assertTrue(ctx.get(facing).isEmpty());
        assertTrue(ctx.get(tier).isEmpty());
    }

    @Test
    void typedKeysAreDistinctByType() {
        var ctx = RuntimeStructureContext.builder(testKey())
                .put(facing, Direction.NORTH)
                .build();
        // ContextKey<string>("facing") ≠ ContextKey<Direction>("facing") — different value type
        assertTrue(ctx.get(ContextKey.string("facing")).isEmpty());
        // But the typed key finds the value
        assertEquals(Direction.NORTH, ctx.get(facing).orElseThrow());
    }

    @Test
    void stringViewRoundTripsAllEntriesAsStrings() {
        var ctx = RuntimeStructureContext.builder(testKey())
                .put(facing, Direction.EAST)
                .put(tier, 5)
                .build();
        var view = ctx.values().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> String.valueOf(e.getValue())));
        assertEquals("east", view.get("facing"));
        assertEquals("5", view.get("tier"));
        assertEquals(2, view.size());
    }

    @Test
    void contextKeyFactoriesCoverCommonTypes() {
        assertEquals(String.class, ContextKey.string("s").type());
        assertEquals(Integer.class, ContextKey.integer("i").type());
        assertEquals(Long.class, ContextKey.longKey("l").type());
        assertEquals(Boolean.class, ContextKey.booleanKey("b").type());
        assertEquals(Double.class, ContextKey.doubleKey("d").type());
        assertEquals(BlockPos.class, ContextKey.blockPos("p").type());
        assertEquals(Direction.class, ContextKey.direction("dir").type());
        assertEquals(ResourceLocation.class, ContextKey.resourceLocation("r").type());
    }

    @Test
    void contextKeyNameMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> ContextKey.string(""));
        assertThrows(IllegalArgumentException.class, () -> ContextKey.string("   "));
        assertThrows(IllegalArgumentException.class, () -> ContextKey.string(null));
    }
}
