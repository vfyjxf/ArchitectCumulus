package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.LinkContext;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;

import java.util.List;
import java.util.Optional;

/**
 * linker：读取依赖上下文并链接为最终运行时上下文。
 *
 * <p>linker 通常在拓扑序上执行；当依赖缺失或缺数据时，应返回 INCOMPLETE 并产出请求/诊断。
 */
public interface Linker {
    LinkReport link(LinkContext ctx);

    /**
     * 默认 no-op：直接返回 compiledContext。
     */
    static Linker none() {
        return ctx -> new LinkReport(InstanceStatus.OK, List.of(), List.of(), InterestUpdate.empty(), Optional.of(ctx.compiledContext()));
    }
}
