package dev.vfyjxf.cumulus.api.binding;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 默认 bindings 实现：以不可变 Map 存储 role -> parts。
 */
public record Bindings(Map<String, List<PartRef>> byRole) implements BindingsView {
    public Bindings {
        byRole = Map.copyOf(Objects.requireNonNull(byRole, "byRole"));
        byRole.values().forEach(Objects::requireNonNull);
    }

    @Override
    public Set<String> roles() {
        return byRole.keySet();
    }

    @Override
    public List<PartRef> parts(String role) {
        Objects.requireNonNull(role, "role");
        return byRole.getOrDefault(role, List.of());
    }
}
