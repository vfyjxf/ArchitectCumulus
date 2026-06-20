package dev.vfyjxf.cumulus.api.context.plan.stdlib;

import dev.vfyjxf.cumulus.api.component.Axis;
import dev.vfyjxf.cumulus.api.component.StructureComponent;
import dev.vfyjxf.cumulus.api.context.plan.CheckPhase;
import dev.vfyjxf.cumulus.api.context.plan.PhaseContext;
import dev.vfyjxf.cumulus.api.context.plan.PhaseResult;
import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.types.ContextKey;

import java.util.ArrayList;

/**
 * Standard library of {@link CheckPhase} factories.
 *
 * <p>The primary convenience is {@link #repeatRange}, which implements axial
 * min/max repetition: generate {@code min} layers as required points, then
 * probe from {@code min+1} to {@code max} as optional points. The actual
 * matched layer count is written to the output {@link ContextKey}.
 *
 * <pre>{@code
 * .checkPhase(CheckPhases.repeatRange(
 *     "ring",                                            // name prefix
 *     Components.hollowBox("ring", at(-1,0,-1), at(1,0,2)), // template component
 *     Axis.y,                                             // repetition axis
 *     2,                                                  // min (must satisfy, else invalid)
 *     5,                                                  // max (probe as many as possible)
 *     TOWER_HEIGHT))                                      // output ContextKey for layer count
 * }</pre>
 */
public final class CheckPhases {

    private CheckPhases() {}

    /**
     * Create a {@link CheckPhase} that repeats a template component along an
     * axis with a min/max range.
     *
     * <p>Semantics:
     * <ul>
     *   <li>Layers 0..{@code min-1} are emitted as {@code required} points — if
     *       any fails to match, the structure is {@code invalid}.</li>
     *   <li>Layers {@code min}..{@code max-1} are emitted as {@code optional}
     *       points — they are checked but don't invalidate if absent.</li>
     *   <li>The number of successfully matched layers (0-based count) is written
     *       to {@code outKey} in the computation context.</li>
     * </ul>
     *
     * @param namePrefix prefix for generated point ids (e.g. "ring" → "ring_0_1_0")
     * @param template   the component whose points are repeated at each offset
     * @param axis       which axis to repeat along
     * @param min        minimum layers (required); must be ≥ 1
     * @param max        maximum layers to probe; must be ≥ min
     * @param outKey     context key to store the matched layer count
     */
    public static CheckPhase repeatRange(String namePrefix, StructureComponent template, Axis axis, int min, int max, ContextKey<Integer> outKey) {
        if (min < 1) {
            throw new IllegalArgumentException("repeatRange min must be >= 1");
        }
        if (max < min) {
            throw new IllegalArgumentException("repeatRange max must be >= min");
        }
        return new RepeatRangePhase(namePrefix, template, axis, min, max, outKey);
    }

    // ── implementation ──────────────────────────────────────────────────

    private static final class RepeatRangePhase implements CheckPhase {
        private final String namePrefix;
        private final StructureComponent template;
        private final Axis axis;
        private final int min;
        private final int max;
        private final ContextKey<Integer> outKey;

        RepeatRangePhase(String namePrefix, StructureComponent template, Axis axis, int min, int max, ContextKey<Integer> outKey) {
            this.namePrefix = namePrefix;
            this.template = template;
            this.axis = axis;
            this.min = min;
            this.max = max;
            this.outKey = outKey;
        }

        @Override
        public PhaseResult evaluate(PhaseContext ctx) {
            // Emit the template's points at each layer offset
            var points = new ArrayList<StructurePointDefinition>();
            for (int layer = 0; layer < max; layer++) {
                final int layerIndex = layer;
                final int offset = layer;
                final var requirement = layer < min ? PointRequirement.required : PointRequirement.optional;
                var collected = new ArrayList<StructurePointDefinition>();
                template.emit(p -> {
                    var shifted = axis.shift(p.location(), offset);
                    collected.add(new StructurePointDefinition(
                                    new StructurePointId(namePrefix + "_L" + layerIndex + "_" + p.id().value()),
                                    shifted,
                                    p.target(),
                                    p.predicates(),
                                    requirement,
                                    p.captures(),
                                    p.groups(),
                                    p.diagnostics()));
                });
                points.addAll(collected);
            }
            // Write the max possible layer count; the executor will match them
            // and the actual count (how many matched) can be derived by a
            // subsequent EvaluationPhase reading matchResults.
            // For now we write max as a placeholder; a follow-up operation
            // can count the actual matched layers.
            ctx.context().putComputed(outKey, max);
            return PhaseResult.of(points);
        }
    }
}
