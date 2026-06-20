package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the read-only query surface of {@link MatchResults} and the
 * {@link MatchedPoint}/{@link Bounds3d} value types it operates on.
 *
 * <p>These are the primitives the generic context-operation pipeline hands to
 * each {@link EvaluationPhase}; their correctness (count/sum/bounds/interior
 * volume, typed property access) is asserted here directly so the pipeline has
 * a trustworthy foundation even before the orchestration layer is wired in.
 */
class MatchResultsTest {

    private static MatchedPoint point(String id, Set<String> groups, String value, Map<String, String> props, int x, int y, int z) {
        return new MatchedPoint(
                        new StructurePointId(id),
                        groups,
                        value,
                        props,
                        RelativeBlockPos.of(x, y, z),
                        new BlockPos(x, y, z));
    }

    @Test
    void countAndSumAggregateOverMatchingPoints() {
        var matches = new MatchResults(List.of(point("c1", Set.of("casing"), "cumulus:casing", Map.of("level", "2"), 0, 0, 0), point("c2", Set.of("casing"), "cumulus:casing", Map.of("level", "5"), 1, 0, 0), point("ctrl", Set.of("controller"), "cumulus:controller", Map.of(), 2, 0, 0)));

        assertEquals(2, matches.count(p -> p.inGroup("casing")));
        assertEquals(1, matches.count(p -> p.inGroup("controller")));
        assertEquals(0, matches.count(p -> p.inGroup("absent")));

        // sum of the int "level" property across casing points (2 + 5)
        assertEquals(7, matches.sumInt(p -> p.inGroup("casing"), p -> p.intProperty("level").orElse(0)));
    }

    @Test
    void firstReturnsEarliestMatchInDeclarationOrder() {
        var matches = new MatchResults(List.of(point("a", Set.of("g"), "v", Map.of(), 0, 0, 0), point("b", Set.of("g"), "v", Map.of(), 1, 0, 0)));

        assertEquals("a", matches.first(p -> p.inGroup("g")).orElseThrow().id().value());
        assertTrue(matches.first(p -> p.inGroup("missing")).isEmpty());
    }

    @Test
    void boundsComputesAxisAlignedBoxOfMatchingPoints() {
        var matches = new MatchResults(List.of(point("c1", Set.of("casing"), "v", Map.of(), 0, 0, 0), point("c2", Set.of("casing"), "v", Map.of(), 2, 2, 2), point("ctrl", Set.of("controller"), "v", Map.of(), 5, 5, 5)));

        var bounds = matches.bounds(p -> p.inGroup("casing")).orElseThrow();
        assertEquals(RelativeBlockPos.of(0, 0, 0), bounds.min());
        assertEquals(RelativeBlockPos.of(2, 2, 2), bounds.max());
        assertEquals(3, bounds.width());
        assertEquals(3, bounds.height());
        assertEquals(3, bounds.depth());
        assertEquals(27, bounds.volume());
        assertTrue(matches.bounds(p -> p.inGroup("missing")).isEmpty());
    }

    @Test
    void interiorVolumeOfHollowShellIsReducedByOneBlockPerSide() {
        // 5x5x5 shell → interior (5-2)^3 = 27
        var shell = new MatchResults(
                        List.of(point("c0", Set.of("casing"), "v", Map.of(), 0, 0, 0), point("c1", Set.of("casing"), "v", Map.of(), 4, 0, 0), point("c2", Set.of("casing"), "v", Map.of(), 0, 4, 0), point("c3", Set.of("casing"), "v", Map.of(), 0, 0, 4), point("c4", Set.of("casing"), "v", Map.of(), 4, 4, 4)));

        assertEquals(27, shell.interiorVolume("casing").orElseThrow());
        // 2x2x2 (each dim < 3) → clamped to 0
        var tiny = new MatchResults(List.of(point("c0", Set.of("casing"), "v", Map.of(), 0, 0, 0), point("c1", Set.of("casing"), "v", Map.of(), 1, 1, 1)));
        assertEquals(0, tiny.interiorVolume("casing").orElseThrow());
        // absent group → empty
        assertTrue(shell.interiorVolume("missing").isEmpty());
    }

    @Test
    void typedPropertyAccessorsParseAndRejectGarbage() {
        var p = point("c", Set.of(), "v", Map.of("n", "7", "d", "1.5", "bad", "xyz"), 0, 0, 0);

        assertEquals(7, p.intProperty("n").orElseThrow());
        assertEquals(1.5, p.property("d", Double.class).orElseThrow(), 1e-9);
        // unparseable int → empty, never throws
        assertTrue(p.intProperty("bad").isEmpty());
        // missing key → empty
        assertTrue(p.intProperty("absent").isEmpty());
    }

    @Test
    void streamAndAllExposeDeclarationOrder() {
        var pts = List.of(point("a", Set.of(), "v", Map.of(), 0, 0, 0), point("b", Set.of(), "v", Map.of(), 1, 0, 0));
        var matches = new MatchResults(pts);

        assertEquals(List.of("a", "b"), matches.all().stream().map(p -> p.id().value()).toList());
        assertEquals(List.of("a", "b"), matches.stream().map(p -> p.id().value()).toList());
        // matching() is a general predicate filter
        assertEquals(1, matches.matching(p -> p.relativePos().x() == 1).count());
    }
}
