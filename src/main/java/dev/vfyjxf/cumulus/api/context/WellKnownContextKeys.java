package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.types.ContextKey;

/**
 * Reserved context keys produced by the runtime itself (not by authoring rules).
 *
 * <p>These were previously inline string literals like {@code "parent_status"}
 * scattered across {@code SubstructureLifecycleState}. Centralising them here
 * removes magic strings and gives consumers a stable typed handle.
 *
 * <p>Reserved namespace: rule authors should not define keys whose name starts
 * with {@code parent_} or {@code substructure.}. The validator will reject
 * such rule definitions at build time once Phase 3 lands.
 */
public final class WellKnownContextKeys {

    private WellKnownContextKeys() {}

    /** Occurrence id of the parent structure (e.g. {@code "factory#0"}). */
    public static final ContextKey<String> parentOccurrence = ContextKey.string("parent_occurrence");

    /** Definition id of the parent structure. */
    public static final ContextKey<String> parentStructure = ContextKey.string("parent_structure");

    /** Absolute anchor position of the parent structure. */
    public static final ContextKey<String> parentAnchor = ContextKey.string("parent_anchor");

    /** Last computed status of the parent structure. */
    public static final ContextKey<String> parentStatus = ContextKey.string("parent_status");
}
