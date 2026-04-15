package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.runtime.RuntimeContextView;

import java.util.Optional;

public interface InstanceDirectory {
    Optional<RuntimeContextView> context(InstanceKey key);

    Optional<InstanceStatus> status(InstanceKey key);
}
