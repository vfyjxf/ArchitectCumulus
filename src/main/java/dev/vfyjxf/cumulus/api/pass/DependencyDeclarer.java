package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.DepContext;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;

import java.util.List;

/**
 * 依赖声明器：声明跨实例只读依赖边。
 *
 * <p>该阶段不直接读取依赖的 runtimeContext（由 linker 阶段负责），只负责声明边
 * （例如：本机器依赖某个邻接机器的输出）。
 */
public interface DependencyDeclarer {
    DependencyReport declare(DepContext ctx);

    /**
     * 默认 no-op 实现：不声明任何依赖。
     */
    static DependencyDeclarer none() {
        return ctx -> new DependencyReport(InstanceStatus.OK, List.of(), List.of(), InterestUpdate.empty(), List.of());
    }
}
