package dev.vfyjxf.cumulus.api.request;

import dev.vfyjxf.cumulus.api.partition.SectionKey;

public record DataRequest(
                SectionKey section,
                RequestReason reason,
                RequestPriority priority,
                int ttlTicks,
                String consumerId) {
    public DataRequest {
        if (section == null) {
            throw new IllegalArgumentException("Request section must not be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Request reason must not be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Request priority must not be null");
        }
        if (ttlTicks < 0) {
            throw new IllegalArgumentException("Request ttl must not be negative");
        }
        if (consumerId == null || consumerId.isBlank()) {
            throw new IllegalArgumentException("Request consumer id must not be blank");
        }
    }
}
