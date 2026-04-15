package dev.vfyjxf.cumulus.api.driver;

import dev.vfyjxf.cumulus.api.engine.ChangeCause;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * driver 输出。
 *
 * <p>引擎会将多个 driver 的输出与外部 {@link dev.vfyjxf.cumulus.api.engine.ChangeSet} 合并：
 * <ul>
 *     <li>{@link #dirtySections()}：触发 anchor 发现与实例重算的最小粒度</li>
 *     <li>{@link #dirtyAnchors()}：可选的更精确 hint</li>
 *     <li>{@link #requests()}：对缺失 section 的建议加载请求</li>
 * </ul>
 */
public record DriverOutput(
        Set<SectionKey> dirtySections,
        Set<BlockPos> dirtyAnchors,
        List<DataRequest> requests,
        ChangeCause cause
) {
    public DriverOutput {
        dirtySections = Set.copyOf(Objects.requireNonNull(dirtySections, "dirtySections"));
        dirtyAnchors = Set.copyOf(Objects.requireNonNull(dirtyAnchors, "dirtyAnchors"));
        requests = List.copyOf(Objects.requireNonNull(requests, "requests"));
        Objects.requireNonNull(cause, "cause");
    }

    public static DriverOutput empty(ChangeCause cause) {
        return new DriverOutput(Set.of(), Set.of(), List.of(), cause);
    }
}
