package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;

@FunctionalInterface
public interface ScheduledCheck {
    ScheduledCheckResult check(StructureInstanceKey instance);
}
