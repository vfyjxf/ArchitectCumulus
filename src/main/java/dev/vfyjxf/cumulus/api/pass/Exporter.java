package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.ExportContext;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.export.ExportView;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;

import java.util.List;
import java.util.Optional;

public interface Exporter {
    ExportReport export(ExportContext ctx);

    static Exporter none() {
        return ctx -> new ExportReport(InstanceStatus.OK, List.of(), List.of(), InterestUpdate.empty(), Optional.empty());
    }
}
