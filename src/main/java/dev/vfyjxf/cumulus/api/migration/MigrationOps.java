package dev.vfyjxf.cumulus.api.migration;

import dev.vfyjxf.cumulus.api.types.ContextKey;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Built-in {@link MigrationOp} factories. Use these in DSL authoring:
 *
 * <pre>{@code
 * .migration(new MigrationDescriptor(
 *     1, 2,
 *     ResourceLocation.parse("cumulus:machine_1_to_2"),
 *     List.of(
 *         MigrationOps.rename(FACING, ORIENTATION),
 *         MigrationOps.addDefault(TIER, 1))))
 * }</pre>
 *
 * For ops with custom Java logic that can't be expressed as data, implement
 * {@link MigrationOp} directly (see {@link TransformValueOp} for an example).
 */
public final class MigrationOps {

    private MigrationOps() {}

    /** Move the value at {@code from} to {@code to}. No-op if {@code from} is absent. */
    public static MigrationOp rename(ContextKey<?> from, ContextKey<?> to) {
        return new RenameKeyOp(from, to);
    }

    /**
     * If {@code key} is absent, populate it with {@code value}. No-op if already present.
     */
    public static <T> MigrationOp addDefault(ContextKey<T> key, T value) {
        return new AddDefaultOp<>(key, value);
    }

    /** Drop the entry at {@code key}. No-op if absent. */
    public static MigrationOp remove(ContextKey<?> key) {
        return new RemoveKeyOp(key);
    }

    /**
     * Apply {@code transform} to the value at {@code key}, storing the result back.
     * Throws {@link IllegalStateException} at apply time if {@code key} is absent.
     *
     * <p>Java DSL only — the function reference can't be serialized to datapack JSON.
     * For datapack-driven transforms, implement {@link MigrationOp} directly and
     * register it by id.
     */
    public static <T> MigrationOp transform(ContextKey<T> key, Function<T, T> transform) {
        return new TransformValueOp<>(key, transform);
    }

    /**
     * Convenience: build a custom op from a Java lambda. The id is used for
     * serialization diagnostics; same caveats as {@link #transform} apply.
     */
    public static MigrationOp custom(ResourceLocation id, Consumer<MutableRuntimeContext> action) {
        return new CustomOp(id, action);
    }

    // ───────────────────────────────────────────────────────────────────
    // Built-in implementations — package-private records, exposed via factory above
    // ───────────────────────────────────────────────────────────────────

    public record RenameKeyOp(ContextKey<?> from, ContextKey<?> to) implements MigrationOp {
        public RenameKeyOp {
            Objects.requireNonNull(from, "rename 'from' key must not be null");
            Objects.requireNonNull(to, "rename 'to' key must not be null");
        }

        @Override
        public ResourceLocation id() {
            return ResourceLocation.parse("cumulus:migration/rename");
        }

        @Override
        public void apply(MutableRuntimeContext ctx) {
            if (!ctx.contains(from)) {
                return;
            }
            var raw = ctx.remove(from).orElse(null);
            if (raw != null) {
                ctx.putRaw(to, raw);
            }
        }
    }

    public record AddDefaultOp<T>(ContextKey<T> key, T value) implements MigrationOp {
        public AddDefaultOp {
            Objects.requireNonNull(key, "addDefault key must not be null");
            Objects.requireNonNull(value, "addDefault value must not be null");
        }

        @Override
        public ResourceLocation id() {
            return ResourceLocation.parse("cumulus:migration/add_default");
        }

        @Override
        public void apply(MutableRuntimeContext ctx) {
            if (!ctx.contains(key)) {
                ctx.put(key, value);
            }
        }
    }

    public record RemoveKeyOp(ContextKey<?> key) implements MigrationOp {
        public RemoveKeyOp {
            Objects.requireNonNull(key, "remove key must not be null");
        }

        @Override
        public ResourceLocation id() {
            return ResourceLocation.parse("cumulus:migration/remove");
        }

        @Override
        public void apply(MutableRuntimeContext ctx) {
            ctx.remove(key);
        }
    }

    public record TransformValueOp<T>(ContextKey<T> key, Function<T, T> transform) implements MigrationOp {
        public TransformValueOp {
            Objects.requireNonNull(key, "transform key must not be null");
            Objects.requireNonNull(transform, "transform function must not be null");
        }

        @Override
        public ResourceLocation id() {
            return ResourceLocation.parse("cumulus:migration/transform_value");
        }

        @Override
        public void apply(MutableRuntimeContext ctx) {
            var current = ctx.<T>get(key).orElseThrow(() -> new IllegalStateException(
                            "TransformValueOp: missing key '" + key.name() + "'"));
            ctx.put(key, transform.apply(current));
        }
    }

    public record CustomOp(
                    ResourceLocation id,
                    Consumer<MutableRuntimeContext> action) implements MigrationOp {
        public CustomOp {
            Objects.requireNonNull(id, "custom op id must not be null");
            Objects.requireNonNull(action, "custom op action must not be null");
        }

        @Override
        public void apply(MutableRuntimeContext ctx) {
            action.accept(ctx);
        }
    }
}
