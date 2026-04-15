package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.LinkContext;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;

import java.util.List;
import java.util.Optional;

public interface Linker {
    LinkReport link(LinkContext ctx);

    static Linker none() {
        return ctx -> new LinkReport(InstanceStatus.OK, List.of(), List.of(), InterestUpdate.empty(), Optional.of(ctx.compiledContext()));
    }
}
