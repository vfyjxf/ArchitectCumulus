package dev.vfyjxf.cumulus.impl.design3;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ExportedRuntimeAsset(
        String resourcePath,
        Map<String, Object> values,
        String serialized) {
    public ExportedRuntimeAsset {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Exported asset resource path must not be blank");
        }
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        if (serialized == null) {
            throw new IllegalArgumentException("Serialized asset content must not be null");
        }
    }
}
