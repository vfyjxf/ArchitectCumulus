package dev.vfyjxf.cumulus.impl.build;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class InMemoryBuildTarget implements BuildTarget {
    private static final ResourceLocation CAPABILITY = ResourceLocation.parse("cumulus:capability");
    private final Map<String, BuiltTarget> targets = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> capabilityMutations = new LinkedHashMap<>();

    @Override
    public boolean supports(BuildAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Build action must not be null");
        }
        return !CAPABILITY.equals(action.targetType())
                || !action.properties().getOrDefault("block", "").isBlank()
                || !action.properties().getOrDefault("operation", "").isBlank();
    }

    @Override
    public boolean contains(BuildAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Build action must not be null");
        }
        if (isCapabilityMutation(action)) {
            return capabilityMutations.containsKey(capabilityMutationKey(action));
        }
        return targets.containsKey(key(action));
    }

    @Override
    public void apply(BuildAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Build action must not be null");
        }
        if (isCapabilityMutation(action)) {
            capabilityMutations.putIfAbsent(capabilityMutationKey(action), mutationProperties(action));
            return;
        }
        var previous = targets.putIfAbsent(
                key(action),
                new BuiltTarget(action.targetType(), action.min(), action.max(), action.id()));
        if (previous != null && !previous.actionId().equals(action.id())) {
            throw new IllegalStateException("Build target already exists for a different action: " + action.id());
        }
    }

    public Map<String, BuiltTarget> targets() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(targets));
    }

    public Map<String, Map<String, String>> capabilityMutations() {
        var values = new LinkedHashMap<String, Map<String, String>>();
        capabilityMutations.forEach((key, properties) -> values.put(key, Map.copyOf(properties)));
        return Collections.unmodifiableMap(values);
    }

    private static String key(BuildAction action) {
        return BuildTargetKey.from(action).toString();
    }

    private static boolean isCapabilityMutation(BuildAction action) {
        return CAPABILITY.equals(action.targetType())
                && !action.properties().getOrDefault("operation", "").isBlank();
    }

    private static String capabilityMutationKey(BuildAction action) {
        return action.targetType()
                + "|" + action.min().asString()
                + "|" + action.max().asString()
                + "|" + action.properties().getOrDefault("name", action.id());
    }

    private static Map<String, String> mutationProperties(BuildAction action) {
        var properties = new LinkedHashMap<String, String>();
        for (var entry : action.properties().entrySet()) {
            if (!entry.getKey().equals("name") && !entry.getKey().equals("block")) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(properties);
    }
}
