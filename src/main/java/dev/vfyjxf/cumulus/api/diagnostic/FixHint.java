package dev.vfyjxf.cumulus.api.diagnostic;

import java.util.Map;

public record FixHint(
        String action,
        String target,
        String componentPath,
        Map<String, String> properties) {
    public FixHint {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Fix action must not be blank");
        }
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Fix target must not be blank");
        }
        if (componentPath == null || componentPath.isBlank()) {
            throw new IllegalArgumentException("Fix component path must not be blank");
        }
        properties = Map.copyOf(properties);
    }
}
