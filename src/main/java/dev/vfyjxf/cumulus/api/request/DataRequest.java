package dev.vfyjxf.cumulus.api.request;

import dev.vfyjxf.cumulus.api.engine.InstanceKey;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record DataRequest(
        SectionKey section,
        ResourceLocation reason,
        RequestPriority priority,
        Duration ttl,
        Optional<InstanceKey> instance,
        String consumerId
) {
    public DataRequest {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(ttl, "ttl");
        instance = Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(consumerId, "consumerId");
    }

    public static DataRequest driver(SectionKey section, ResourceLocation reason, RequestPriority priority, Duration ttl, String consumerId) {
        return new DataRequest(section, reason, priority, ttl, Optional.empty(), consumerId);
    }

    public static DataRequest forInstance(SectionKey section, ResourceLocation reason, RequestPriority priority, Duration ttl, InstanceKey instance, String consumerId) {
        return new DataRequest(section, reason, priority, ttl, Optional.of(instance), consumerId);
    }
}
