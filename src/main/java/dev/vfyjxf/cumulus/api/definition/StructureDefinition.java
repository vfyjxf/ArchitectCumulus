package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.component.ComponentDescriptor;
import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.migration.MigrationDescriptor;
import dev.vfyjxf.cumulus.api.migration.StructureVersionDescriptor;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record StructureDefinition(
        ResourceLocation id,
        int schemaVersion,
        List<TransformDescriptor> transforms,
        List<StructurePointDefinition> points,
        List<ComponentDescriptor> components,
        List<ContextRuleDescriptor<?>> contextRules,
        List<StructureVersionDescriptor> versions,
        List<MigrationDescriptor> migrations,
        DiagnosticMetadata diagnostics) {
    public static final int IR_SCHEMA_VERSION = 2;

    public StructureDefinition {
        if (id == null) throw new IllegalArgumentException("Structure id must not be null");
        transforms = List.copyOf(transforms);
        points = List.copyOf(points);
        components = List.copyOf(components);
        contextRules = List.copyOf(contextRules);
        versions = List.copyOf(versions);
        migrations = List.copyOf(migrations);
        if (diagnostics == null) throw new IllegalArgumentException("Diagnostics must not be null");
    }

    public StructureDefinition(
            ResourceLocation id,
            List<TransformDescriptor> transforms,
            List<StructurePointDefinition> points,
            List<ContextRuleDescriptor<?>> contextRules,
            DiagnosticMetadata diagnostics) {
        this(id, IR_SCHEMA_VERSION, transforms, points, List.of(), contextRules, List.of(), List.of(), diagnostics);
    }
}
