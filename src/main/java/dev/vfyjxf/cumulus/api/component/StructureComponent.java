package dev.vfyjxf.cumulus.api.component;

public interface StructureComponent {
    String name();

    void emit(ComponentEmitter emitter);
}
