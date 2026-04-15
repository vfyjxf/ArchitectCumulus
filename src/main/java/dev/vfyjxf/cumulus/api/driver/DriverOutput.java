package dev.vfyjxf.cumulus.api.driver;

import dev.vfyjxf.cumulus.api.engine.ChangeCause;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record DriverOutput(
        Set<SectionKey> dirtySections,
        Set<BlockPos> dirtyAnchors,
        List<DataRequest> requests,
        ChangeCause cause
) {
    public DriverOutput {
        dirtySections = Set.copyOf(Objects.requireNonNull(dirtySections, "dirtySections"));
        dirtyAnchors = Set.copyOf(Objects.requireNonNull(dirtyAnchors, "dirtyAnchors"));
        requests = List.copyOf(Objects.requireNonNull(requests, "requests"));
        Objects.requireNonNull(cause, "cause");
    }

    public static DriverOutput empty(ChangeCause cause) {
        return new DriverOutput(Set.of(), Set.of(), List.of(), cause);
    }
}
