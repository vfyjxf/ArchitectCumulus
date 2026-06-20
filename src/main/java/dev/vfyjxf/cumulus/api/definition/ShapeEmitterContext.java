package dev.vfyjxf.cumulus.api.definition;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Sink for {@link ShapeEmitter} — receives emitted
 * {@link StructurePointDefinition}s. Each call to {@link #point} corresponds to
 * one cell in the shape matrix being expanded.
 */
public final class ShapeEmitterContext {

    private final Consumer<StructurePointDefinition> sink;

    public ShapeEmitterContext(Consumer<StructurePointDefinition> sink) {
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
    }

    /**
     * Emit a point at the given relative position using the part pattern's
     * own name + coordinates as the point id.
     */
    public void point(int x, int y, int z, PartPattern part) {
        var pos = RelativeBlockPos.of(x, y, z);
        sink.accept(part.toPoint(pos));
    }

    /**
     * Emit a point with a custom id at the given relative position.
     */
    public void point(String id, int x, int y, int z, PartPattern part) {
        sink.accept(part.toPoint(id, RelativeBlockPos.of(x, y, z)));
    }

    /**
     * Emit a raw point definition directly (for custom emitters that need
     * full control).
     */
    public void point(StructurePointDefinition point) {
        sink.accept(point);
    }

    /**
     * Convenience: create a context that feeds into a list-based consumer.
     */
    public static ShapeEmitterContext toList(List<StructurePointDefinition> list) {
        return new ShapeEmitterContext(list::add);
    }
}
