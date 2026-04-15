package dev.vfyjxf.cumulus.api.engine;

import dev.vfyjxf.cumulus.api.ArchitectSchema;

import java.util.Optional;

/**
 * 结构检查/编译引擎（每个 Level/维度通常一份）。
 *
 * <p>引擎负责：
 * <ul>
 *     <li>根据 dirty sections/anchors 找到受影响的结构实例</li>
 *     <li>在预算（{@link Budget}）内运行 schema 的流水线（bind/check/compile/...）</li>
 *     <li>输出实例状态变更（{@link InstanceDelta}）与缺数据请求（{@link dev.vfyjxf.cumulus.api.request.DataRequest}）</li>
 * </ul>
 *
 * <p>缺数据语义：当某个 pass 需要的 world 数据未加载时，应返回 {@code InstanceStatus.INCOMPLETE}，
 * 并通过 {@link dev.vfyjxf.cumulus.api.context.PassContext#request} 产出 {@code DataRequest}。引擎本身不强制加载区块，
 * 只把请求返回给宿主系统处理。
 *
 * <p>线程模型：v1 设计默认单线程调用（register/submit/tick），更复杂的并行执行可在后续版本扩展。
 */
public interface ArchitectEngine {
    /**
     * 注册一个结构类型（schema）。
     *
     * <p>同一个 id 重复注册时的覆盖/合并策略由具体实现决定（dummy 实现会覆盖）。
     */
    void register(ArchitectSchema schema);

    /**
     * 提交一次变化提示（可选）。
     *
     * <p>当宿主拥有事件流（方块更新/实体变化等）时，用 {@link ChangeSet} 提示引擎增量收敛；
     * 当没有事件流时，可依赖 driver 主动扫描（见 {@link dev.vfyjxf.cumulus.api.driver.DriverStrategy}）。
     */
    void submit(ChangeSet changeSet);

    /**
     * 推进一步引擎状态（一个 tick）。
     *
     * <p>实现应在 {@link TickContext#budget()} 允许的成本内尽量完成工作；当预算不足时可以提前返回，
     * 下个 tick 继续收敛。
     */
    EngineFrame tick(TickContext ctx);

    /**
     * 获取某个实例的最新视图（如果存在）。
     */
    Optional<InstanceView> instance(InstanceKey key);
}
