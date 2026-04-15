package dev.vfyjxf.cumulus.api.engine;

public interface EngineInstanceDirectory extends InstanceDirectory {
    Iterable<InstanceView> views();
}

