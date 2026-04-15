package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.engine.Budget;
import dev.vfyjxf.cumulus.api.engine.InstanceDirectory;
import dev.vfyjxf.cumulus.api.engine.InstanceKey;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;

public interface PassContext {
    InstanceKey key();

    PartitionAccess world();

    InstanceDirectory instances();

    Budget budget();

    PassSink sink();

    default ResourceLocation schemaId() {
        return key().schemaId();
    }

    default BlockPos anchorPos() {
        return key().anchorPos();
    }

    default void request(SectionKey section, ResourceLocation reason, RequestPriority priority, Duration ttl, String consumerId) {
        sink().request(DataRequest.forInstance(section, reason, priority, ttl, key(), consumerId));
    }

    default void diag(Diagnostic diagnostic) {
        sink().diag(diagnostic);
    }

    default void interest(InterestUpdate update) {
        sink().interest(update);
    }
}
