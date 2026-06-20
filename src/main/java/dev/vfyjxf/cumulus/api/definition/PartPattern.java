package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.target.TargetPattern;
import dev.vfyjxf.cumulus.api.target.Targets;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Pre-bound part pattern — the complete definition of one cell in a structure
 * shape matrix. Holds the {@link TargetPattern} (target + predicates + captures),
 * {@link PointRequirement}, and group tags.
 *
 * <p>Designed for two usage modes:
 *
 * <p><b>1. Static constant</b> (preferred for reusable parts):
 * <pre>{@code
 * public static final PartPattern CASING = PartPattern.block("casing")
 *     .where(PredicateDescriptors.tag("cumulus:heat_casing"))
 *     .group("casing");
 * public static final PartPattern CTRL = PartPattern.block("controller")
 *     .where(PredicateDescriptors.id("cumulus:controller"))
 *     .capture("controller");
 * }</pre>
 *
 * <p><b>2. Inline in array</b> (for one-off structures):
 * <pre>{@code
 * .shape(new PartPattern[][][]{{
 *     { PartPattern.block("c").where(tag("cumulus:casing")),
 *       PartPattern.block("h").where(id("cumulus:controller")) },
 * }})
 * }</pre>
 *
 * <p>Use {@link #air()} for cells that should be skipped (empty / optional).
 */
public final class PartPattern {

    private final String name;
    private final TargetPattern target;
    private final PointRequirement requirement;
    private final Set<String> groups;
    private final DiagnosticMetadata diagnostics;

    private PartPattern(String name, TargetPattern target, PointRequirement requirement, Set<String> groups, DiagnosticMetadata diagnostics) {
        this.name = Objects.requireNonNull(name, "PartPattern name must not be null");
        if (name.isBlank())
            throw new IllegalArgumentException("PartPattern name must not be blank");
        this.target = Objects.requireNonNull(target, "PartPattern target must not be null");
        this.requirement = Objects.requireNonNull(requirement, "PartPattern requirement must not be null");
        this.groups = Set.copyOf(groups);
        this.diagnostics = diagnostics == null ? DiagnosticMetadata.empty() : diagnostics;
    }

    // ── Factory: from TargetPattern ───────────────────────────────────

    public static PartPattern of(TargetPattern target) {
        return new PartPattern("part", target, PointRequirement.required, Set.of(), DiagnosticMetadata.empty());
    }

    public static PartPattern of(String name, TargetPattern target) {
        return new PartPattern(name, target, PointRequirement.required, Set.of(), DiagnosticMetadata.empty());
    }

    // ── Factory: convenient presets ───────────────────────────────────

    /** A required block target named {@code name}. */
    public static PartPattern block(String name) {
        return of(name, Targets.block());
    }

    /** A required block target, name defaults to "block". */
    public static PartPattern block() {
        return block("block");
    }

    /** A required block-entity target. */
    public static PartPattern blockEntity(String name) {
        return of(name, Targets.blockEntity());
    }

    /** A required capability target. */
    public static PartPattern capability(String name, String capabilityName) {
        return of(name, Targets.capability(capabilityName));
    }

    /** A required fluid target. */
    public static PartPattern fluid(String name) {
        return of(name, Targets.fluid());
    }

    /** A required entity target. */
    public static PartPattern entity(String name) {
        return of(name, Targets.entity());
    }

    /** An optional air/any cell — skipped by the default emitter. */
    public static PartPattern air() {
        return new PartPattern("air", Targets.block(), PointRequirement.optional, Set.of(), DiagnosticMetadata.empty());
    }

    // ── Fluent modifiers (return new PartPattern) ─────────────────────

    public PartPattern target(TargetPattern target) {
        return new PartPattern(name, target, requirement, groups, diagnostics);
    }

    public PartPattern where(PredicateDescriptor predicate) {
        return new PartPattern(name, target.where(predicate), requirement, groups, diagnostics);
    }

    public PartPattern capture(String captureName) {
        return new PartPattern(name, target.capture(captureName), requirement, groups, diagnostics);
    }

    public PartPattern optional() {
        return new PartPattern(name, target, PointRequirement.optional, groups, diagnostics);
    }

    public PartPattern required() {
        return new PartPattern(name, target, PointRequirement.required, groups, diagnostics);
    }

    public PartPattern group(String group) {
        var next = new LinkedHashSet<>(groups);
        next.add(group);
        return new PartPattern(name, target, requirement, next, diagnostics);
    }

    /** Attach diagnostic metadata (flows to DiagnosticNode evidence). */
    public PartPattern metadata(DiagnosticMetadata meta) {
        return new PartPattern(name, target, requirement, groups, meta);
    }

    // ── Accessors ─────────────────────────────────────────────────────

    public String name() {
        return name;
    }

    public TargetPattern target() {
        return target;
    }

    public PointRequirement requirement() {
        return requirement;
    }

    public DiagnosticMetadata diagnostics() {
        return diagnostics;
    }

    public Set<String> groups() {
        return groups;
    }

    /** Whether this pattern is the {@link #air()} sentinel. */
    public boolean isAir() {
        return "air".equals(name) && requirement == PointRequirement.optional;
    }

    // ── Expand to StructurePointDefinition ────────────────────────────

    /**
     * Materialize this pattern into a {@link StructurePointDefinition} at the
     * given relative position. The point id is derived from {@code name + "_" + x + "_" + y + "_" + z}.
     */
    public StructurePointDefinition toPoint(RelativeBlockPos pos) {
        return toPoint(name + "_" + pos.x() + "_" + pos.y() + "_" + pos.z(), pos);
    }

    /**
     * Materialize this pattern into a {@link StructurePointDefinition} with a
     * custom id at the given relative position.
     */
    public StructurePointDefinition toPoint(String id, RelativeBlockPos pos) {
        return new StructurePointDefinition(
                        new StructurePointId(id),
                        pos,
                        target.target(),
                        target.predicates(),
                        requirement,
                        target.captures(),
                        groups,
                        DiagnosticMetadata.empty());
    }

    @Override
    public String toString() {
        return "PartPattern[" + name + "]";
    }
}
