package dev.vfyjxf.cumulus.api.component;

import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;

@FunctionalInterface
public interface ComponentEmitter {
    void point(StructurePointDefinition point);
}
