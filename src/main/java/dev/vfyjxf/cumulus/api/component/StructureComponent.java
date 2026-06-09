package dev.vfyjxf.cumulus.api.component;

import java.util.Optional;

public interface StructureComponent {
    String name();

    default Optional<ComponentDescriptor> descriptor() {
        return Optional.empty();
    }

    void emit(ComponentEmitter emitter);
}
