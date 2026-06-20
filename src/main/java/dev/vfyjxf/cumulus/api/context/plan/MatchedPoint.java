package dev.vfyjxf.cumulus.api.context.plan;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Typed view of one matched structure point, exposed to {@link ContextOperation}s
 * through {@link MatchResults}.
 *
 * <p>This is the runtime counterpart of {@code StructurePointDefinition} after a
 * candidate has matched: the id, group membership, primary resolved value, the
 * resolved target's property map, and the point's position in both relative
 * (structure-local) and absolute (world) coordinates.
 *
 * @param id           the point identifier
 * @param groups       group tags assigned to this point (may be empty)
 * @param value        the resolved target's primary value (e.g. block id); never null
 * @param properties   resolved target property map (e.g. {@code facing=north}); never null
 * @param relativePos  structure-local position
 * @param absolutePos  world position after the chosen transform is applied
 */
public record MatchedPoint(
                StructurePointId id,
                Set<String> groups,
                String value,
                Map<String, String> properties,
                RelativeBlockPos relativePos,
                BlockPos absolutePos) {

    public MatchedPoint {
        Objects.requireNonNull(id, "MatchedPoint id must not be null");
        Objects.requireNonNull(value, "MatchedPoint value must not be null");
        Objects.requireNonNull(properties, "MatchedPoint properties must not be null");
        Objects.requireNonNull(relativePos, "MatchedPoint relativePos must not be null");
        Objects.requireNonNull(absolutePos, "MatchedPoint absolutePos must not be null");
        groups = Set.copyOf(groups);
        properties = Map.copyOf(properties);
    }

    /**
     * Typed property read. Returns empty if the property is absent or its string
     * value cannot be parsed as {@code type}.
     */
    public <T> Optional<T> property(String name, Class<T> type) {
        var raw = properties.get(name);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            if (type == String.class)
                return Optional.of(type.cast(raw));
            if (type == Integer.class)
                return Optional.of(type.cast(Integer.valueOf(raw)));
            if (type == Long.class)
                return Optional.of(type.cast(Long.valueOf(raw)));
            if (type == Double.class)
                return Optional.of(type.cast(Double.valueOf(raw)));
            if (type == Boolean.class)
                return Optional.of(type.cast(Boolean.valueOf(raw)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /** Convenience for the common {@code int} property case. */
    public Optional<Integer> intProperty(String name) {
        return property(name, Integer.class);
    }

    /** True if this point belongs to the named group. */
    public boolean inGroup(String group) {
        return groups.contains(group);
    }
}
