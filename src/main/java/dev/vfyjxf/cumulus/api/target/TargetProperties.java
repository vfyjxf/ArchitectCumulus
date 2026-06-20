package dev.vfyjxf.cumulus.api.target;

/**
 * Marker interface for the typed properties record carried by a {@link TargetType}.
 * Each concrete target type declares its own Properties record implementing this
 * interface, replacing the previous {@code Map<String, String>} bag.
 *
 * <p>Example:
 * <pre>{@code
 * public final class BlockEntityTargetType implements TargetType {
 *     public record Props(
 *             ResourceLocation blockId,
 *             ResourceLocation blockEntityId,
 *             String nbt) implements TargetProperties {}
 * }
 * }</pre>
 */
public interface TargetProperties {}
