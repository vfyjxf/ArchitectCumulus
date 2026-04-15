package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.request.DataRequest;

import java.util.List;
import java.util.Objects;

/**
 * 引擎每 tick 的输出帧。
 *
 * <p>包含：
 * <ul>
 *     <li>{@link #deltas()}：实例新增/更新/移除的快照变更</li>
 *     <li>{@link #newRequests()}：本 tick 产生的新缺数据请求（实现可去重/节流）</li>
 * </ul>
 */
public record EngineFrame(List<InstanceDelta> deltas, List<DataRequest> newRequests) {
    public EngineFrame {
        deltas = List.copyOf(Objects.requireNonNull(deltas, "deltas"));
        newRequests = List.copyOf(Objects.requireNonNull(newRequests, "newRequests"));
    }

    /**
     * 空输出。
     */
    public static EngineFrame empty() {
        return new EngineFrame(List.of(), List.of());
    }
}
