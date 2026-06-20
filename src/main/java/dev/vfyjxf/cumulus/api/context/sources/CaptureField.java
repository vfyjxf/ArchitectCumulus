package dev.vfyjxf.cumulus.api.context.sources;

import dev.vfyjxf.cumulus.api.context.CheckContext;

import java.util.Objects;

/**
 * Which field of a {@link CheckContext.ResolvedTarget} a {@link CaptureSource}
 * reads. Replaces the three former source types (the former CaptureValueSource,
 * CapturePositionSource, and CapturePropertySource with a discriminated field.
 *
 * <p>Sealed interface with three permitted records so the codec can
 * pattern-match for serialization without probe-testing.
 */
public sealed interface CaptureField
                permits CaptureField.Value, CaptureField.Position, CaptureField.Property {

    String extract(CheckContext.ResolvedTarget target);

    /** Read the primary value (block/fluid/entity id). */
    record Value() implements CaptureField {
        @Override
        public String extract(CheckContext.ResolvedTarget target) {
            return target.value();
        }
    }

    /** Read the absolute world position string. */
    record Position() implements CaptureField {
        @Override
        public String extract(CheckContext.ResolvedTarget target) {
            return target.absolutePosition();
        }
    }

    /** Read a named property from the target's property map. */
    record Property(String name) implements CaptureField {
        public Property {
            Objects.requireNonNull(name, "property name must not be null");
        }

        @Override
        public String extract(CheckContext.ResolvedTarget target) {
            return target.properties().get(name);
        }
    }

    /** Factory: value field. */
    static CaptureField value() {
        return new Value();
    }

    /** Factory: position field. */
    static CaptureField position() {
        return new Position();
    }

    /** Factory: named property field. */
    static CaptureField property(String name) {
        return new Property(name);
    }
}
