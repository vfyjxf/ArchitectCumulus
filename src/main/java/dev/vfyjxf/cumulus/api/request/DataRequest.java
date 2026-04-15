package dev.vfyjxf.cumulus.api.request;

import dev.vfyjxf.cumulus.api.engine.InstanceKey;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 缺数据请求（建议）。
 *
 * <p>当某个 pass 需要某个 {@link SectionKey} 的世界数据但该 section 未加载时，
 * pass 应返回 {@code INCOMPLETE} 并通过 {@link dev.vfyjxf.cumulus.api.context.PassContext#request}
 * 产出一个 {@code DataRequest}。引擎只负责把请求汇总/返回给宿主，不负责强制加载。
 *
 * <p>字段含义：
 * <ul>
 *     <li>{@link #section()}：最小请求粒度（默认 MC section 16×16×16）</li>
 *     <li>{@link #reason()}：稳定原因码（用于日志/节流/聚合）</li>
 *     <li>{@link #priority()}：建议优先级（宿主可映射到 ticket/队列优先级）</li>
 *     <li>{@link #ttl()}：该请求在多长时间内可被认为“仍然有效”</li>
 *     <li>{@link #instance()}：可选实例归因（哪个结构实例提出的请求）</li>
 *     <li>{@link #consumerId()}：更细粒度的归因（哪个 pass/driver）</li>
 * </ul>
 */
public record DataRequest(
        SectionKey section,
        ResourceLocation reason,
        RequestPriority priority,
        Duration ttl,
        Optional<InstanceKey> instance,
        String consumerId
) {
    public DataRequest {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(ttl, "ttl");
        instance = Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(consumerId, "consumerId");
    }

    /**
     * driver 级请求（不归因到某个实例）。
     */
    public static DataRequest driver(SectionKey section, ResourceLocation reason, RequestPriority priority, Duration ttl, String consumerId) {
        return new DataRequest(section, reason, priority, ttl, Optional.empty(), consumerId);
    }

    /**
     * 实例级请求（归因到某个结构实例）。
     */
    public static DataRequest forInstance(SectionKey section, ResourceLocation reason, RequestPriority priority, Duration ttl, InstanceKey instance, String consumerId) {
        return new DataRequest(section, reason, priority, ttl, Optional.of(instance), consumerId);
    }
}
