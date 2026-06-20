package dev.vfyjxf.cumulus.api.diagnostic;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry of all known {@link DiagnosticCode}s. Built-in codes are pre-registered
 * (see {@link StandardDiagnosticCodes}); mods add their own during common setup:
 *
 * <pre>{@code
 * MinecraftRuntimeEventBridge.BUS.addListener((FMLCommonSetupEvent e) ->
 *     e.enqueueWork(() ->
 *         DiagnosticCodes.register(MyModCodes.MY_CUSTOM)));
 * }</pre>
 *
 * <p>Lookups ({@link #fromId(ResourceLocation)}) never throw — unknown ids fall
 * back to a placeholder so a partially-loaded modpack never breaks the diagnostic
 * pipeline. The placeholder carries the original id in its
 * {@link SimpleDiagnosticCode#localizationKey()} so users still see a meaningful label.
 */
public final class DiagnosticCodes {

    private static final Map<ResourceLocation, DiagnosticCode> entries = new LinkedHashMap<>();

    static {
        // register all standard codes eagerly so they're available before any mod setup runs
        for (var code : StandardDiagnosticCodes.all) {
            entries.put(code.id(), code);
        }
    }

    private DiagnosticCodes() {}

    /**
     * Register a custom code. Re-registering the same id replaces the entry.
     * Must be called from a setup event handler, not during a tick.
     */
    public static synchronized void register(DiagnosticCode code) {
        Objects.requireNonNull(code, "DiagnosticCode must not be null");
        entries.put(code.id(), code);
    }

    /** Look up by id; empty if no code registered under that id. */
    public static Optional<DiagnosticCode> maybeFromId(ResourceLocation id) {
        Objects.requireNonNull(id, "DiagnosticCode id must not be null");
        return Optional.ofNullable(entries.get(id));
    }

    /**
     * Look up by id; if unknown, returns a fallback {@link SimpleDiagnosticCode}
     * with {@link Severity#WARN} and a synthesized localization key, so callers
     * never see a null/throw.
     */
    public static DiagnosticCode fromId(ResourceLocation id) {
        var existing = entries.get(id);
        if (existing != null) {
            return existing;
        }
        return new SimpleDiagnosticCode(
                        id,
                        Severity.warn,
                        "diagnostic.unknown." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
    }
}
