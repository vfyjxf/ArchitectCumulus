package dev.vfyjxf.cumulus.api.migration;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record MigrationDescriptor(
        int fromVersion,
        int toVersion,
        ResourceLocation migrationId,
        Map<String, String> properties) {
    public MigrationDescriptor {
        if (fromVersion < 1) {
            throw new IllegalArgumentException("Migration source version must be positive");
        }
        if (toVersion < 1) {
            throw new IllegalArgumentException("Migration target version must be positive");
        }
        if (fromVersion >= toVersion) {
            throw new IllegalArgumentException("Migration target version must be newer than source version");
        }
        if (migrationId == null) {
            throw new IllegalArgumentException("Migration id must not be null");
        }
        properties = Map.copyOf(properties);
    }
}
