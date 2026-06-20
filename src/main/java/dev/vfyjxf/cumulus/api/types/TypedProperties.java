package dev.vfyjxf.cumulus.api.types;

/**
 * Marker interface for typed property bags carried by descriptors
 * (TargetProperties, PredicateProperties, TransformProperties, …).
 *
 * <p>Each concrete descriptor type (e.g. {@code BlockEntityTargetType}) declares its own
 * {@code record Properties implements TargetProperties { … }} with strongly-typed fields.
 * This interface is intentionally empty: the type system itself is the contract.
 *
 * <p>This is the open-for-extension counterpart to a sealed hierarchy. Mods and datapack
 * authors can introduce new {@code TypedProperties} subtypes without modifying Cumulus.
 */
public interface TypedProperties {
}
