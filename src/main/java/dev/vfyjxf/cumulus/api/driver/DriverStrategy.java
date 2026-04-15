package dev.vfyjxf.cumulus.api.driver;

public interface DriverStrategy {
    DriverOutput run(DriverContext ctx);
}
