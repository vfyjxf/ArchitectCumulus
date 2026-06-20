package dev.vfyjxf.cumulus.impl.codec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record RuntimeAssetSource(
                String resourcePath,
                Map<String, Object> values) {
    public RuntimeAssetSource {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Runtime asset source path must not be blank");
        }
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
