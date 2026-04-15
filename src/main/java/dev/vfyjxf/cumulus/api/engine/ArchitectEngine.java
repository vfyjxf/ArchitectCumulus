package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.ArchitectSchema;

import java.util.Optional;

public interface ArchitectEngine {
    void register(ArchitectSchema schema);

    void submit(ChangeSet changeSet);

    EngineFrame tick(TickContext ctx);

    Optional<InstanceView> instance(InstanceKey key);
}
