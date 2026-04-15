package dev.vfyjxf.cumulus.api.driver;

import dev.vfyjxf.cumulus.api.engine.Budget;
import dev.vfyjxf.cumulus.api.engine.EngineInstanceDirectory;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;

public interface DriverContext {
    long tick();

    PartitionAccess world();

    EngineInstanceDirectory instances();

    Budget budget();
}
