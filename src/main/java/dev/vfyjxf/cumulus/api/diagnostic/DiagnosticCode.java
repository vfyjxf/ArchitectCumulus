package dev.vfyjxf.cumulus.api.diagnostic;

import net.minecraft.resources.ResourceLocation;

/**
 * Strongly-typed diagnostic code. Replaces the previous bare-string convention
 * where each call site wrote {@code "MISSING_TARGET"} / {@code "PREDICATE_FAILED"}
 * etc. inline.
 *
 * <p>Open interface: mods and datapacks can register their own codes via
 * {@link DiagnosticCodes#register}. Built-in codes live in
 * {@link StandardDiagnosticCodes} and cover the 11 well-known categories.
 *
 * <p>Serialization: codes are serialized by {@link #id()} (a {@link ResourceLocation}).
 * On reload, {@link DiagnosticCodes#fromId(ResourceLocation)} resolves the id back
 * to the typed instance; unknown ids fall back to a generic placeholder so a
 * missing mod never crashes the diagnostic pipeline.
 */
public interface DiagnosticCode {

    /** Stable identifier; used as the wire-format key and i18n lookup base. */
    ResourceLocation id();

    /**
     * Default severity when the diagnostic node doesn't override.
     * Callers may still escalate or downgrade per-instance.
     */
    Severity defaultSeverity();

    /**
     * Localization key for the user-facing message. Conventionally
     * {@code "diagnostic.<namespace>.<path>"}.
     */
    String localizationKey();
}
