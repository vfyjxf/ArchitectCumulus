package dev.vfyjxf.cumulus.common.driver;

import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.driver.DriverContext;
import dev.vfyjxf.cumulus.api.driver.DriverOutput;
import dev.vfyjxf.cumulus.api.driver.DriverStrategy;
import dev.vfyjxf.cumulus.api.engine.ChangeCause;
import dev.vfyjxf.cumulus.api.engine.InstanceView;
import dev.vfyjxf.cumulus.api.partition.LoadState;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * INCOMPLETE 预取请求策略：遍历引擎中状态为 {@code INCOMPLETE} 的实例，
 * 对其兴趣集（interest.sections）内缺失的 section 产出 {@link DataRequest}。
 *
 * <p>注意：该策略只“请求”，不“加载”。加载由宿主系统决定。
 */
public final class RequestingPrefetchScanDriver implements DriverStrategy {
    private static final ResourceLocation DEFAULT_REASON = ResourceLocation.fromNamespaceAndPath("cumulus", "prefetch_missing_section");

    private final ResourceLocation reason;
    private final RequestPriority priority;
    private final Duration ttl;
    private final String consumerId;

    public RequestingPrefetchScanDriver(ResourceLocation reason, RequestPriority priority, Duration ttl, String consumerId) {
        this.reason = Objects.requireNonNull(reason, "reason");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.consumerId = Objects.requireNonNull(consumerId, "consumerId");
    }

    public static RequestingPrefetchScanDriver defaultForIncomplete() {
        return new RequestingPrefetchScanDriver(DEFAULT_REASON, RequestPriority.NORMAL, Duration.ofSeconds(10), "prefetch_driver");
    }

    @Override
    public DriverOutput run(DriverContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        List<DataRequest> requests = new ArrayList<>();

        // 遍历所有实例，寻找 INCOMPLETE 的并对缺失 section 发请求。
        for (InstanceView view : ctx.instances().views()) {
            if (view.status() != InstanceStatus.INCOMPLETE) {
                continue;
            }
            for (SectionKey section : view.interest().sections()) {
                if (!ctx.budget().tryConsume(1)) {
                    return new DriverOutput(Set.of(), Set.of(), requests, ChangeCause.SCAN);
                }
                if (ctx.world().loadState(section) == LoadState.MISSING) {
                    requests.add(DataRequest.forInstance(section, reason, priority, ttl, view.key(), consumerId));
                }
            }
        }

        return new DriverOutput(Set.of(), Set.of(), requests, ChangeCause.SCAN);
    }
}
