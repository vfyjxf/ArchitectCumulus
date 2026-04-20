package dev.vfyjxf.cumulus.api.component;

import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.target.Targets;
import java.util.Set;

public final class Components {
    private Components() {
    }

    public static PointComponent point(String name, RelativeBlockPos location) {
        return new PointComponent(name, location, Targets.block(), PointRequirement.REQUIRED, Set.of(), DiagnosticMetadata.empty());
    }

    public static BoxComponent box(String name, RelativeBlockPos from, RelativeBlockPos to) {
        return new BoxComponent(name, from, to, false, Targets.block(), PointRequirement.REQUIRED, Set.of(), DiagnosticMetadata.empty());
    }

    public static BoxComponent hollowBox(String name, RelativeBlockPos from, RelativeBlockPos to) {
        return new BoxComponent(name, from, to, true, Targets.block(), PointRequirement.REQUIRED, Set.of(), DiagnosticMetadata.empty());
    }
}
