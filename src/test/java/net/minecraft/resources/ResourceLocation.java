package net.minecraft.resources;

import java.util.Objects;

public final class ResourceLocation {
    private final String namespace;
    private final String path;

    private ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Resource location must not be null");
        }
        var separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException("Invalid resource location: " + value);
        }
        var namespace = value.substring(0, separator);
        var path = value.substring(separator + 1);
        if (namespace.isBlank() || path.isBlank()) {
            throw new IllegalArgumentException("Invalid resource location: " + value);
        }
        return new ResourceLocation(namespace, path);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResourceLocation other)) {
            return false;
        }
        return namespace.equals(other.namespace) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
