package dev.vfyjxf.cumulus.api.migration;

import net.minecraft.resources.ResourceLocation;

/**
 * Single typed migration step applied to a {@link MutableRuntimeContext} during
 * a version-to-version upgrade. Replaces the previous {@code Map<String, String>}
 * properties bag + stringly-typed {@code "operation"} dispatch.
 *
 * <h2>Built-in ops</h2>
 * See {@link MigrationOps} for the standard factories:
 * <ul>
 *   <li>{@link MigrationOps#rename rename} — move a value from one key to another</li>
 *   <li>{@link MigrationOps#addDefault addDefault} — populate a missing key with a fallback</li>
 *   <li>{@link MigrationOps#remove remove} — drop a key</li>
 *   <li>{@link MigrationOps#transform transform} — apply a Java function to a value (Java DSL only)</li>
 * </ul>
 *
 * <h2>Custom ops</h2>
 * Implement this interface directly, then the op will work in both DSL and datapack
 * (identified by its {@link #id()}).
 *
 * <pre>{@code
 * public final class MyCustomOp implements MigrationOp {
 *     public ResourceLocation id() { return ResourceLocation.parse("mymod:custom"); }
 *     public void apply(MutableRuntimeContext ctx) {
 *         // ...
 *     }
 * }
 * }</pre>
 */
public interface MigrationOp {

    /** Stable identifier — used as the wire-format key for datapack JSON. */
    ResourceLocation id();

    /**
     * Mutate {@code ctx} in place. Implementations must be idempotent: applying
     * the same op twice to the same context should produce the same result.
     */
    void apply(MutableRuntimeContext ctx);
}
