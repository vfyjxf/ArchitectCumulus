package dev.vfyjxf.cumulus.api.diagnostic;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Reference {@link DiagnosticCode} implementation. Mods that just need to declare
 * a custom code without overriding any behaviour should use this record directly:
 *
 * <pre>{@code
 * public static final DiagnosticCode myCustom =
 *     new SimpleDiagnosticCode(
 *         ResourceLocation.parse("mymod:custom_failure"),
 *         Severity.warn,
 *         "diagnostic.mymod.custom_failure");
 * }</pre>
 *
 * For richer behaviour (e.g. code-specific localisation params), implement
 * {@link DiagnosticCode} directly.
 */
public record SimpleDiagnosticCode(
                ResourceLocation id,
                Severity defaultSeverity,
                String localizationKey) implements DiagnosticCode {

    public SimpleDiagnosticCode {
        Objects.requireNonNull(id, "DiagnosticCode id must not be null");
        Objects.requireNonNull(defaultSeverity, "DiagnosticCode severity must not be null");
        if (localizationKey == null || localizationKey.isBlank()) {
            throw new IllegalArgumentException("DiagnosticCode localizationKey must not be blank");
        }
    }
}
