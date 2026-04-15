package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.ExportContext;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.export.ExportView;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;

import java.util.List;
import java.util.Optional;

/**
 * exporter：导出视图（用于可视化/数据生产的数据访问，不做 UI）。
 */
public interface Exporter {
    ExportReport export(ExportContext ctx);

    /**
     * 默认 no-op：不导出任何视图。
     */
    static Exporter none() {
        return ctx -> new ExportReport(InstanceStatus.OK, List.of(), List.of(), InterestUpdate.empty(), Optional.empty());
    }
}
