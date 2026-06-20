package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.types.ContextKey;

import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextMapTest {

    private static final ContextKey<Direction> facing = ContextKey.direction("facing");
    private static final ContextKey<Integer> tier = ContextKey.integer("tier");

    @Test
    void builderProducesImmutableMap() {
        var ctx = ContextMap.builder().put(facing, Direction.NORTH).put(tier, 3).build();
        assertEquals(Direction.NORTH, ctx.get(facing).orElseThrow());
        assertEquals(3, ctx.get(tier).orElseThrow());
        assertTrue(ctx.get(ContextKey.string("absent")).isEmpty());
    }

    @Test
    void mutableAllowsPutAndRemove() {
        var mut = ContextMap.mutable();
        mut.put(facing, Direction.EAST);
        assertTrue(ctxContains(facing, mut));
        mut.remove(facing);
        assertFalse(mut.contains(facing));
    }

    @Test
    void freezeProducesImmutable() {
        var mut = ContextMap.mutable();
        mut.put(tier, 5);
        var frozen = mut.freeze();
        assertEquals(5, frozen.get(tier).orElseThrow());
        // Original mutable still usable
        mut.put(tier, 10);
        assertEquals(5, frozen.get(tier).orElseThrow());
    }

    @Test
    void putRejectsMismatchedType() {
        var mut = ContextMap.mutable();
        @SuppressWarnings("rawtypes")
        ContextKey rawKey = facing;
        assertThrows(ClassCastException.class, () -> mut.putRaw(rawKey, "not_a_direction"));
    }

    @Test
    void putRawEnforcesRuntimeCheck() {
        var mut = ContextMap.mutable();
        @SuppressWarnings("rawtypes")
        ContextKey rawKey = tier;
        assertThrows(ClassCastException.class, () -> mut.putRaw(rawKey, "wrong"));
    }

    @Test
    void mutableCopySeedsFromImmutable() {
        var orig = ContextMap.builder().put(tier, 3).build();
        var copy = orig.mutableCopy();
        copy.put(tier, 7);
        assertEquals(3, orig.get(tier).orElseThrow());
        assertEquals(7, copy.get(tier).orElseThrow());
    }

    @Test
    void equalsComparesEntries() {
        var a = ContextMap.builder().put(tier, 3).build();
        var b = ContextMap.builder().put(tier, 3).build();
        assertEquals(a, b);
    }

    private static boolean ctxContains(ContextKey<?> key, ContextMap.Mutable mut) {
        return mut.contains(key);
    }
}
