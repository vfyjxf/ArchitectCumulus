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
 *   <li><b>computed</b> — values produced by {@code ContextOperation}s in a
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
    private final Map<ContextKey<?>, Object> derived = new LinkedHashMap<>();

    /** Computed values from ContextPlan operations. Keyed by typed ContextKey.
     *  Carried over into RuntimeStructureContext; shadows derived under the same key. */
    private final Map<ContextKey<?>, Object> computed = new LinkedHashMap<>();

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
        Objects.requireNonNull(key, "derived key must not be null");
        Objects.requireNonNull(value, "derived value must not be null");
        if (!key.type().isInstance(value)) {
            throw new ClassCastException(
                            "ContextKey '" + key.name() + "' expects " + key.type().getName() + " but got " + value.getClass().getName());
        }
        derived.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> derivedValue(ContextKey<T> key) {
        var raw = derived.get(key);
        return raw == null ? Optional.empty() : Optional.of((T) raw);
    }

    /** True if any derived value exists under {@code key}. */
    public boolean hasDerived(ContextKey<?> key) {
        return derived.containsKey(key);
    }

    // ── computed (ContextPlan operations → runtime context) ──────────────

    public <T> void putComputed(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "computed key must not be null");
        Objects.requireNonNull(value, "computed value must not be null");
        if (!key.type().isInstance(value)) {
            throw new ClassCastException(
                            "ContextKey '" + key.name() + "' expects " + key.type().getName() + " but got " + value.getClass().getName());
        }
        computed.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> computedValue(ContextKey<T> key) {
        var raw = computed.get(key);
        return raw == null ? Optional.empty() : Optional.of((T) raw);
    }

    /** True if any computed value exists under {@code key}. */
    public boolean hasComputed(ContextKey<?> key) {
        return computed.containsKey(key);
    }

    /**
     * Cross-tier lookup: return the value under {@code key} from the computed
     * tier if present, otherwise from the derived tier. This is the read view
     * exposed to {@code ContextOperation}s via {@code OperationContext} — an
     * operation can consume another operation's output or a context rule's
     * derived value without knowing which tier produced it.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> anyValue(ContextKey<T> key) {
        var raw = computed.get(key);
        if (raw == null) {
            raw = derived.get(key);
        }
        return raw == null ? Optional.empty() : Optional.of((T) raw);
    }

    // ── conversion ────────────────────────────────────────────────────

    /**
     * Produce the immutable machine-facing snapshot. Both {@code derived} and
     * {@code computed} entries are carried over (computed applied last, so it
     * shadows derived under the same key); resolvedTargets and captures are dropped.
     */
    public RuntimeStructureContext toRuntime(StructureInstanceKey key, int revision) {
        var merged = new LinkedHashMap<ContextKey<?>, Object>();
        merged.putAll(derived);
        merged.putAll(computed);
        return RuntimeStructureContext.builder(key).revision(revision).putAll(merged).build();
    }

    /**
     * Copy all tiers from another CheckContext (for sub-structure composition
     * where a child's derived values become the parent's captures).
     */
    public void absorb(CheckContext other) {
        this.resolvedTargets.putAll(other.resolvedTargets);
        this.captures.putAll(other.captures);
        this.derived.putAll(other.derived);
        this.computed.putAll(other.computed);
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
