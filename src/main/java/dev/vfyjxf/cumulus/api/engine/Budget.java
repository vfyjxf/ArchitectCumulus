package dev.vfyjxf.cumulus.api.engine;

/**
 * tick 预算。
 *
 * <p>预算用于将各种扫描/检查成本统一量化（“代价单位”由实现自行定义，但应保持一致），
 * 从而避免在无事件流或大范围扫描时卡顿。
 *
 * <p>约定：调用者在执行每个“可能昂贵”的操作前先 {@link #tryConsume(long)}；
 * 如果返回 {@code false}，应尽快停止本次工作并等待下个 tick。
 */
public interface Budget {
    /**
     * 尝试消耗一定成本。
     *
     * @return 是否成功消耗；失败表示预算不足
     */
    boolean tryConsume(long cost);

    /**
     * 当前剩余预算（单位同 {@link #tryConsume(long)} 的 cost）。
     */
    long remaining();

    /**
     * 无限预算（测试/演示用；生产环境通常不建议）。
     */
    static Budget unlimited() {
        return new Budget() {
            @Override
            public boolean tryConsume(long cost) {
                return true;
            }

            @Override
            public long remaining() {
                return Long.MAX_VALUE;
            }
        };
    }
}
