package dev.vfyjxf.cumulus.api.component;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public record SubstructureComponent(
                String name,
                ResourceLocation structureId,
                RelativeBlockPos anchorOffset) implements StructureComponent {
    public SubstructureComponent {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Substructure component name must not be blank");
        }
        if (structureId == null) {
            throw new IllegalArgumentException("Substructure id must not be null");
        }
        if (anchorOffset == null) {
            throw new IllegalArgumentException("Substructure anchor offset must not be null");
        }
    }

    @Override
    public Optional<ComponentDescriptor> descriptor() {
        return Optional.of(new ComponentDescriptor(
                        name,
                        ResourceLocation.parse("cumulus:substructure"),
                        Map.of("structure", structureId, "anchor_offset", anchorOffset.asString())));
    }

    @Override
    public void emit(ComponentEmitter emitter) {}
}
