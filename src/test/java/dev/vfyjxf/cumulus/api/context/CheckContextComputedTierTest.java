package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.types.ContextKey;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code computed} tier of {@link CheckContext}: typed put/get, the
 * {@code anyValue} cross-tier lookup (computed shadows derived), and the
 * {@code derived ∪ computed} merge that {@code toRuntime} produces.
 */
class CheckContextComputedTierTest {

    private static final ContextKey<Integer> casingCount = ContextKey.integer("casing_count");
    private static final ContextKey<Double> factor = ContextKey.doubleKey("factor");
    private static final ContextKey<String> facing = ContextKey.string("facing");

    private static StructureInstanceKey testKey() {
        return new StructureInstanceKey(ResourceLocation.parse("cumulus:test"), new BlockPos(0, 64, 0));
    }

    @Test
    void putComputedStoresTypedValueAndComputedValueReadsIt() {
        var ctx = new CheckContext();
        assertTrue(ctx.computedValue(casingCount).isEmpty());
        ctx.putComputed(casingCount, 9);
        assertEquals(9, ctx.computedValue(casingCount).orElseThrow());
        assertTrue(ctx.hasComputed(casingCount));
    }

    @Test
    void putComputedRejectsMismatchedValueType() {
        var ctx = new CheckContext();
        // Raw type to defeat the static check (deserialization paths hit this guard at runtime).
        @SuppressWarnings("rawtypes")
        ContextKey rawKey = factor;
        assertThrows(ClassCastException.class, () -> ctx.putComputed(rawKey, "not a double"));
    }

    @Test
    void anyValueFallsBackFromComputedToDerived() {
        var ctx = new CheckContext();
        ctx.putDerived(facing, "north");
        // Not yet computed: anyValue reads derived.
        assertEquals(Optional.of("north"), ctx.anyValue(facing));
        // After computing under the same key, computed shadows derived.
        ctx.putComputed(facing, "east");
        assertEquals(Optional.of("east"), ctx.anyValue(facing));
        // derived tier still holds the original value.
        assertFalse(ctx.derivedValue(facing).isEmpty());
    }

    @Test
    void toRuntimeMergesDerivedAndComputed() {
        var ctx = new CheckContext();
        ctx.putDerived(facing, "north");
        ctx.putComputed(casingCount, 9);
        ctx.putComputed(factor, 2.0);

        var runtime = ctx.toRuntime(testKey(), 1);

        assertEquals("north", runtime.get(facing).orElseThrow());
        assertEquals(9, runtime.get(casingCount).orElseThrow());
        assertEquals(2.0, runtime.get(factor).orElseThrow(), 1e-9);
    }

    @Test
    void toRuntimeComputedShadowsDerivedUnderSameKey() {
        var ctx = new CheckContext();
        ctx.putDerived(casingCount, 3); // stale value
        ctx.putComputed(casingCount, 9); // operation recomputed it

        var runtime = ctx.toRuntime(testKey(), 0);

        assertEquals(9, runtime.get(casingCount).orElseThrow());
    }

    @Test
    void absorbCopiesComputedTier() {
        var child = new CheckContext();
        child.putComputed(casingCount, 9);
        child.putDerived(facing, "north");

        var parent = new CheckContext();
        parent.absorb(child);

        assertEquals(9, parent.computedValue(casingCount).orElseThrow());
        assertEquals("north", parent.derivedValue(facing).orElseThrow());
        assertTrue(parent.hasComputed(casingCount));

        var runtime = parent.toRuntime(testKey(), 0);
        var expected = RuntimeStructureContext.builder(testKey()).put(casingCount, 9).put(facing, "north").build();
        assertEquals(expected, runtime);
    }
}
