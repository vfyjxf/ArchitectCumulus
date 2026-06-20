package dev.vfyjxf.cumulus.api.definition;

/**
 * Expands a {@code PartPattern[][][]}} shape matrix into
 * {@link StructurePointDefinition}s. The default implementation
 * ({@link DefaultShapeEmitter}) walks the array [y][z][x] and emits one point
 * per non-air cell. Custom implementations can implement arbitrary layout logic.
 *
 * @see DefaultShapeEmitter
 */
@FunctionalInterface
public interface ShapeEmitter {

    /**
     * Expand the shape, calling {@code ctx.point(...)} for each cell that should
     * become a structure point.
     *
     * @param shape the 3D array indexed as [y][z][x]
     * @param ctx   the sink to receive emitted points
     */
    void emit(PartPattern[][][] shape, ShapeEmitterContext ctx);
}
