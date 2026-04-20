package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record StructureDefinition(
        ResourceLocation id,
        int schemaVersion,
        List<TransformDescriptor> transforms,
        List<StructurePointDefinition> points,
        List<ContextRuleDescriptor> contextRules,
        DiagnosticMetadata diagnostics) {
    public static final int IR_SCHEMA_VERSION = 1;

    public StructureDefinition {
        if (id == null) throw new IllegalArgumentException("Structure id must not be null");
        transforms = List.copyOf(transforms);
        points = List.copyOf(points);
        contextRules = List.copyOf(contextRules);
        if (diagnostics == null) throw new IllegalArgumentException("Diagnostics must not be null");
    }

    public StructureDefinition(
            ResourceLocation id,
            List<TransformDescriptor> transforms,
            List<StructurePointDefinition> points,
            List<ContextRuleDescriptor> contextRules,
            DiagnosticMetadata diagnostics) {
        this(id, IR_SCHEMA_VERSION, transforms, points, contextRules, diagnostics);
    }
}
