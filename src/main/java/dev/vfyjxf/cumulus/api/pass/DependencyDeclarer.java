package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.DepContext;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;

import java.util.List;

public interface DependencyDeclarer {
    DependencyReport declare(DepContext ctx);

    static DependencyDeclarer none() {
        return ctx -> new DependencyReport(InstanceStatus.OK, List.of(), List.of(), InterestUpdate.empty(), List.of());
    }
}
