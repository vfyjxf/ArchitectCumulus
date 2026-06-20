package dev.vfyjxf.cumulus.api.diagnostic;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The 17 standard {@link DiagnosticCode}s that Cumulus emits from its built-in
 * check / runtime / serialization paths. All codes use the {@code cumulus:}
 * namespace and live in this class as {@code public static final} constants so
 * static-import ergonomic call sites work:
 *
 * <pre>{@code
 * import static dev.vfyjxf.cumulus.api.diagnostic.StandardDiagnosticCodes.*;
 *
 * nodes.add(new DiagnosticNode(
 *     "controller", missingTarget, "missing controller block", ...));
 * }</pre>
 *
 * <p>Mods that need their own categories should declare additional
 * {@link SimpleDiagnosticCode} constants in their own class and register them
 * via {@link DiagnosticCodes#register}.
 */
public final class StandardDiagnosticCodes {

    private StandardDiagnosticCodes() {}

    // ───────────────────────────────────────────────────────────────────────
    // 11 standard codes — one constant per category, behaviour via SimpleDiagnosticCode
    // ───────────────────────────────────────────────────────────────────────

    /** Required point's location had no world target at all (e.g. air, no entity). */
    public static final DiagnosticCode missingTarget = make("cumulus:missing_target", Severity.error, "diagnostic.cumulus.missing_target");

    /** Target was present but its runtime type didn't match the descriptor. */
    public static final DiagnosticCode wrongTargetType = make("cumulus:wrong_target_type", Severity.error, "diagnostic.cumulus.wrong_target_type");

    /** Target type matched but the predicate chain did not. */
    public static final DiagnosticCode predicateFailed = make("cumulus:predicate_failed", Severity.error, "diagnostic.cumulus.predicate_failed");

    /** Context rule referenced a capture name that wasn't produced by any point. */
    public static final DiagnosticCode contextCaptureFailed = make("cumulus:context_capture_failed", Severity.error, "diagnostic.cumulus.context_capture_failed");

    /** Context rule's source/derived value could not be resolved or computed. */
    public static final DiagnosticCode contextDerivationFailed = make("cumulus:context_derivation_failed", Severity.error, "diagnostic.cumulus.context_derivation_failed");

    /** None of the declared transform candidates could make the structure match. */
    public static final DiagnosticCode transformRejected = make("cumulus:transform_rejected", Severity.warn, "diagnostic.cumulus.transform_rejected");

    /** Two required points occupied the same (location, target) slot at build time. */
    public static final DiagnosticCode componentConflict = make("cumulus:component_conflict", Severity.error, "diagnostic.cumulus.component_conflict");

    /** Substructure references a child definition id that isn't registered. */
    public static final DiagnosticCode dependencyUnresolved = make("cumulus:dependency_unresolved", Severity.error, "diagnostic.cumulus.dependency_unresolved");

    /** Two parent occurrences tried to exclusively claim the same child occurrence. */
    public static final DiagnosticCode sharedPartConflict = make("cumulus:shared_part_conflict", Severity.error, "diagnostic.cumulus.shared_part_conflict");

    /** Target was in an unloaded section; check deferred, request emitted. */
    public static final DiagnosticCode unloadedArea = make("cumulus:unloaded_area", Severity.info, "diagnostic.cumulus.unloaded_area");

    /** Predicate descriptor uses a registry id with no matcher registered. */
    public static final DiagnosticCode runtimeOnlyUnavailable = make("cumulus:runtime_only_unavailable", Severity.warn, "diagnostic.cumulus.runtime_only_unavailable");

    // ───────────────────────────────────────────────────────────────────────
    // Additional runtime categories beyond the original 11 — kept here so
    // impl/ call sites have a single typed source for these well-known cases
    // instead of inlining bespoke SimpleDiagnosticCode instances.
    // ───────────────────────────────────────────────────────────────────────

    /** Datapack / datagen asset failed to load (IO error, missing file). */
    public static final DiagnosticCode assetLoadFailed = make("cumulus:asset_load_failed", Severity.error, "diagnostic.cumulus.asset_load_failed");

    /** Asset content couldn't be parsed (invalid JSON / NBT). */
    public static final DiagnosticCode invalidAssetContent = make("cumulus:invalid_asset_content", Severity.error, "diagnostic.cumulus.invalid_asset_content");

    /** Build executor was asked to perform an unsupported action. */
    public static final DiagnosticCode unsupportedBuildAction = make("cumulus:unsupported_build_action", Severity.error, "diagnostic.cumulus.unsupported_build_action");

    /** Structure context migration could not be applied. */
    public static final DiagnosticCode migrationFailed = make("cumulus:migration_failed", Severity.error, "diagnostic.cumulus.migration_failed");

    /** Substructure graph contains a cycle. */
    public static final DiagnosticCode substructureCycle = make("cumulus:substructure_cycle", Severity.error, "diagnostic.cumulus.substructure_cycle");

    /** Substructure graph references a missing child or root definition. */
    public static final DiagnosticCode missingDefinition = make("cumulus:missing_definition", Severity.error, "diagnostic.cumulus.missing_definition");

    /** All standard codes in declaration order, for registry bootstrap. */
    public static final List<DiagnosticCode> all = List.of(missingTarget, wrongTargetType, predicateFailed, contextCaptureFailed, contextDerivationFailed, transformRejected, componentConflict, dependencyUnresolved, sharedPartConflict, unloadedArea, runtimeOnlyUnavailable, assetLoadFailed, invalidAssetContent, unsupportedBuildAction, migrationFailed, substructureCycle, missingDefinition);

    private static DiagnosticCode make(String id, Severity severity, String localizationKey) {
        return new SimpleDiagnosticCode(ResourceLocation.parse(id), severity, localizationKey);
    }
}
