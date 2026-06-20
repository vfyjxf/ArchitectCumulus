package dev.vfyjxf.cumulus.api.definition;

/**
 * Default {@link ShapeEmitter}: walks the {@code PartPattern[][][]} array
 * indexed as [y][z][x], emitting one {@link StructurePointDefinition} per
 * non-null, non-air cell. Air cells ({@link PartPattern#air()} or null) are
 * skipped.
 *
 * <p>Custom emitters can implement {@link ShapeEmitter} for arbitrary layout
 * logic (mirroring, rotation, conditional cells, etc.).
 */
public final class DefaultShapeEmitter implements ShapeEmitter {

    public static final DefaultShapeEmitter instance = new DefaultShapeEmitter();

    private DefaultShapeEmitter() {}

    @Override
    public void emit(PartPattern[][][] shape, ShapeEmitterContext ctx) {
        for (int y = 0; y < shape.length; y++) {
            for (int z = 0; z < shape[y].length; z++) {
                for (int x = 0; x < shape[y][z].length; x++) {
                    var part = shape[y][z][x];
                    if (part == null || part.isAir()) {
                        continue;
                    }
                    ctx.point(x, y, z, part);
                }
            }
        }
    }
}
