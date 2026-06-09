package dev.vfyjxf.cumulus.impl.definition;

import dev.vfyjxf.cumulus.api.component.ComponentDescriptor;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructureDefinitionBuilder;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import java.util.ArrayList;
import java.util.List;

public final class StructureDefinitionCompiler {
    private StructureDefinitionCompiler() {
    }

    public static StructureDefinition compile(StructureDefinitionBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Structure definition builder must not be null");
        }

        var transforms = builder.transforms();
        if (transforms.isEmpty()) {
            transforms = List.of(Transforms.identity());
        }

        var points = new ArrayList<StructurePointDefinition>();
        var componentDescriptors = new ArrayList<ComponentDescriptor>();
        for (var point : builder.points()) {
            points.add(point.build());
        }
        for (var component : builder.components()) {
            component.descriptor().ifPresent(componentDescriptors::add);
            component.emit(points::add);
        }

        var definition = new StructureDefinition(
                builder.id(),
                StructureDefinition.IR_SCHEMA_VERSION,
                transforms,
                points,
                componentDescriptors,
                builder.contextRules(),
                builder.versions(),
                builder.migrations(),
                DiagnosticMetadata.empty());
        StructureDefinitionValidator.validate(definition);
        return definition;
    }
}
