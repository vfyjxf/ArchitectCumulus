package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.definition.StructurePointId;
import dev.vfyjxf.cumulus.api.runtime.RuntimeStructureContext;
import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;
import dev.vfyjxf.cumulus.api.types.ContextKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable computation context used during structure checking. Separates the
 * <em>computation phase</em> (resolving targets, evaluating predicates, deriving
 * context values) from the <em>result phase</em> ({@link RuntimeStructureContext}).
 *
 * <h2>Four storage tiers</h2>
 * <ol>
 *   <li><b>resolvedTargets</b> — per-point snapshot data matched during check.
 *       Internal, never exposed to machines.</li>
 *   <li><b>captures</b> — values captured by point.captures() (block positions,
 *       primary values). Internal, used by {@link ContextSource} resolution.</li>
 *   <li><b>derived</b> — values produced by {@link ContextRuleDescriptor} sources.
 *       Carried over into {@link RuntimeStructureContext}.</li>
 *   <li><b>computed</b> — values produced by {@link EvaluationPhase}s in a
 *       {@code ContextPlan}. Also carried over into {@link RuntimeStructureContext};
 *       visible to later operations via {@link #anyValue}.</li>
 * </ol>
 *
 * <p>Both {@code derived} and {@code computed} survive into the runtime context.
 * {@link #anyValue} searches computed first (operations run after rules, so a
 * later operation's output shadows an earlier one under the same key) then
 * derived, giving operations a unified read view.
 *
 * <h2>Lifecycle</h2>
 * <pre>{@code
 * var ctx = new CheckContext();
 * // 1. check each point → putResolvedTarget + putCapture
 * // 2. apply context rules → source.resolve(ctx) → putDerived
 * // 3. run ContextPlan operations → putComputed (can read anyValue)
 * // 4. convert (merges derived + computed)
 * var runtime = ctx.toRuntime(instanceKey, revision);
 * }</pre>
 */
public final class CheckContext {

    /** Matched-point snapshot data. Keyed by point id. */
    private final Map<StructurePointId, ResolvedTarget> resolvedTargets = new LinkedHashMap<>();

    /** Capture values from point.captures(). Keyed by capture name (string). */
    private final Map<String, Object> captures = new LinkedHashMap<>();

    /** Derived values from context rules. Keyed by typed ContextKey.
     *  Carried over into RuntimeStructureContext. */
    private final ContextMap.Mutable derived = ContextMap.mutable();

    /** Computed values from EvaluationPhase. Keyed by typed ContextKey.
     *  Carried over into RuntimeStructureContext; shadows derived under the same key. */
    private final ContextMap.Mutable computed = ContextMap.mutable();

    public CheckContext() {}

    // ── resolved targets ──────────────────────────────────────────────

    public void putResolvedTarget(StructurePointId point, ResolvedTarget target) {
        Objects.requireNonNull(point, "point must not be null");
        Objects.requireNonNull(target, "target must not be null");
        resolvedTargets.put(point, target);
    }

    public Optional<ResolvedTarget> resolvedTarget(StructurePointId point) {
        return Optional.ofNullable(resolvedTargets.get(point));
    }

    // ── captures ──────────────────────────────────────────────────────

    public void putCapture(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("capture name must not be blank");
        }
        Objects.requireNonNull(value, "capture value must not be null");
        captures.put(name, value);
    }

    public Optional<Object> capture(String name) {
        return Optional.ofNullable(captures.get(name));
    }

    // ── derived (→ runtime context) ───────────────────────────────────

    public <T> void putDerived(ContextKey<T> key, T value) {
        derived.put(key, value);
    }

    public <T> Optional<T> derivedValue(ContextKey<T> key) {
        return derived.get(key);
    }

    /** True if any derived value exists under {@code key}. */
    public boolean hasDerived(ContextKey<?> key) {
        return derived.contains(key);
    }

    // ── computed (EvaluationPhase → runtime context) ──────────────────

    public <T> void putComputed(ContextKey<T> key, T value) {
        computed.put(key, value);
    }

    public <T> Optional<T> computedValue(ContextKey<T> key) {
        return computed.get(key);
    }

    /** True if any computed value exists under {@code key}. */
    public boolean hasComputed(ContextKey<?> key) {
        return computed.contains(key);
    }

    /**
     * Cross-tier lookup: return the value under {@code key} from the computed
     * tier if present, otherwise from the derived tier.
     */
    public <T> Optional<T> anyValue(ContextKey<T> key) {
        var computedVal = computed.get(key);
        return computedVal.isPresent() ? computedVal : derived.get(key);
    }

    // ── conversion ────────────────────────────────────────────────────

    /**
     * Produce the immutable machine-facing snapshot. Both {@code derived} and
     * {@code computed} entries are carried over (computed applied last, so it
     * shadows derived under the same key); resolvedTargets and captures are dropped.
     */
    public RuntimeStructureContext toRuntime(StructureInstanceKey key, int revision) {
        var merged = new LinkedHashMap<ContextKey<?>, Object>();
        merged.putAll(derived.entries());
        merged.putAll(computed.entries());
        return RuntimeStructureContext.builder(key).revision(revision).putAll(merged).build();
    }

    /**
     * Copy all tiers from another CheckContext (for sub-structure composition
     * where a child's derived values become the parent's captures).
     */
    public void absorb(CheckContext other) {
        this.resolvedTargets.putAll(other.resolvedTargets);
        this.captures.putAll(other.captures);
        this.derived.putAll(other.derived.entries());
        this.computed.putAll(other.computed.entries());
    }

    /**
     * Snapshot of one matched point: the target's primary value, its property map,
     * and the absolute world position after the chosen transform.
     */
    public record ResolvedTarget(String value, Map<String, String> properties, String absolutePosition) {
        public ResolvedTarget {
            properties = Map.copyOf(properties);
        }
    }
}
