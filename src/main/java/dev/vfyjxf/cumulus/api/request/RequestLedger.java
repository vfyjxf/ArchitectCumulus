package dev.vfyjxf.cumulus.api.request;

import dev.vfyjxf.cumulus.api.partition.SectionKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RequestLedger {
    private final Map<RequestKey, Integer> expiresAtByKey = new HashMap<>();

    public List<DataRequest> record(int tick, int maxRequests, DataRequest... requests) {
        if (maxRequests < 0) {
            throw new IllegalArgumentException("Max requests must not be negative");
        }

        var accepted = new ArrayList<DataRequest>();
        for (var request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("Request must not be null");
            }
            var key = RequestKey.from(request);
            var expiresAt = expiresAtByKey.get(key);
            if (expiresAt != null && expiresAt > tick) {
                continue;
            }
            expiresAtByKey.put(key, tick + request.ttlTicks());
            accepted.add(request);
        }

        accepted.sort(Comparator.comparing(DataRequest::priority).reversed().thenComparing(request -> request.section().x()).thenComparing(request -> request.section().y()).thenComparing(request -> request.section().z()).thenComparing(DataRequest::consumerId));
        if (accepted.size() <= maxRequests) {
            return List.copyOf(accepted);
        }
        return List.copyOf(accepted.subList(0, maxRequests));
    }

    private record RequestKey(
                    SectionKey section,
                    RequestReason reason,
                    String consumerId) {
        private static RequestKey from(DataRequest request) {
            return new RequestKey(request.section(), request.reason(), request.consumerId());
        }
    }
}
