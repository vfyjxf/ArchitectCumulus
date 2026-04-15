package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.Map;
import java.util.Objects;

public record FixHint(String kind, String summary, Map<String, String> params) {
    public FixHint {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(summary, "summary");
        params = Map.copyOf(Objects.requireNonNull(params, "params"));
    }
}
