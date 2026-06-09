package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.target.Targets;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructureDefinitionRegistryTest {
    @Test
    void registersDefinitionsForIdOnlyRuntimeAssetResolution() {
        var registry = new StructureDefinitionRegistry();
        var definition = Structures.define(ResourceLocation.parse("cumulus:registry_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .build();

        assertEquals(Optional.empty(), registry.resolve(definition.id()));

        registry.register(definition);

        assertEquals(Optional.of(definition), registry.resolve(definition.id()));
        assertEquals(List.of(definition), registry.definitions());
    }

    @Test
    void structuresConvenienceApiRegistersDefinitionsInGlobalRegistry() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:global_registry_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .build();

        Structures.register(definition);

        assertEquals(Optional.of(definition), Structures.resolve(definition.id()));
    }
}
