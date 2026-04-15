package dev.vfyjxf.cumulus.api.context;

import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.engine.Budget;
import dev.vfyjxf.cumulus.api.engine.InstanceDirectory;
import dev.vfyjxf.cumulus.api.engine.InstanceKey;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;

/**
 * pass 共享上下文能力。
 *
 * <p>所有 pass 都至少能拿到：
 * <ul>
 *     <li>当前实例键（schemaId + anchorPos）</li>
 *     <li>世界只读访问（PartitionAccess）</li>
 *     <li>跨实例只读访问（InstanceDirectory）</li>
 *     <li>本 tick 预算（Budget）</li>
 *     <li>向引擎回传 requests/diagnostics/interest 的 sink</li>
 * </ul>
 *
 * <p>该接口提供若干便捷方法（schemaId/anchorPos/request/diag/interest）。
 */
public interface PassContext {
    InstanceKey key();

    PartitionAccess world();

    InstanceDirectory instances();

    Budget budget();

    PassSink sink();

    default ResourceLocation schemaId() {
        return key().schemaId();
    }

    default BlockPos anchorPos() {
        return key().anchorPos();
    }

    /**
     * 产出一个缺数据请求（建议宿主加载该 section）。
     */
    default void request(SectionKey section, ResourceLocation reason, RequestPriority priority, Duration ttl, String consumerId) {
        sink().request(DataRequest.forInstance(section, reason, priority, ttl, key(), consumerId));
    }

    /**
     * 产出一条诊断。
     */
    default void diag(Diagnostic diagnostic) {
        sink().diag(diagnostic);
    }

    /**
     * 更新兴趣集（增量）。
     */
    default void interest(InterestUpdate update) {
        sink().interest(update);
    }
}
