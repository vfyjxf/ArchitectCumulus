package dev.vfyjxf.cumulus.api.predicate;

import dev.vfyjxf.cumulus.api.context.plan.MatchedPoint;

/**
 * Factory methods for {@link StructurePredicate} — strongly-typed predicates
 * that test {@link MatchedPoint} values after matching.
 *
 * <pre>{@code
 * // "is a casing and heat-resistant, but not a controller"
 * StructurePredicates.id("cumulus:casing")
 *     .and(StructurePredicates.property("heat_resistant", "true"))
 *     .and(StructurePredicates.id("cumulus:controller").negate())
 * }</pre>
 */
public final class StructurePredicates {

    private StructurePredicates() {}

    /** Match points whose primary value (block id) equals {@code blockId}. */
    public static StructurePredicate id(String blockId) {
        return point -> blockId.equals(point.value());
    }

    /** Match points whose properties contain {@code key=value}. */
    public static StructurePredicate property(String key, String value) {
        return point -> value.equals(point.properties().get(key));
    }

    /** Match points belonging to the named group. */
    public static StructurePredicate group(String groupName) {
        return point -> point.groups().contains(groupName);
    }

    /** Match points with any value (always true). */
    public static StructurePredicate any() {
        return point -> true;
    }

    /** Match points with a specific int property value. */
    public static StructurePredicate intProperty(String key, int value) {
        return point -> point.intProperty(key).orElse(-1) == value;
    }

    /** Match points whose id (StructurePointId value) equals {@code pointId}. */
    public static StructurePredicate pointId(String pointId) {
        return point -> pointId.equals(point.id().value());
    }
}
