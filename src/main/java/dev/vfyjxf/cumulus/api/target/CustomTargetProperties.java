package dev.vfyjxf.cumulus.api.target;

import java.util.Map;

/**
 * Optional interface for {@link TargetProperties} implementations that need
 * to provide a custom back-compat {@code Map<String, String>} view (e.g. for
 * legacy serialization or logging).
 *
 * <p>Built-in target types are handled directly by {@link PropertyExtractor};
 * mods that want to support the back-compat shape for their custom properties
 * record should implement this interface on the record.
 */
public interface CustomTargetProperties extends TargetProperties {

    /** Lossy string-keyed view of this properties record. */
    Map<String, String> asMap();
}
