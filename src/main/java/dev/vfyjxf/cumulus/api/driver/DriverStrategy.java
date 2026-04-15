package dev.vfyjxf.cumulus.api.driver;

/**
 * 主动扫描策略（driver）。
 *
 * <p>driver 应遵循 budget：每次扫描/遍历都消耗预算，预算不足时尽快返回。
 */
public interface DriverStrategy {
    /**
     * 执行一次 driver。
     */
    DriverOutput run(DriverContext ctx);
}
