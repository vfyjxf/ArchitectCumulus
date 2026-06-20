package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.component.StructureComponent;
import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.migration.MigrationDescriptor;
import dev.vfyjxf.cumulus.api.migration.StructureVersionDescriptor;
import dev.vfyjxf.cumulus.api.target.TargetPattern;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import dev.vfyjxf.cumulus.impl.definition.StructureDefinitionCompiler;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StructureDefinitionBuilder {
    private final ResourceLocation id;
    private final List<TransformDescriptor> transforms = new ArrayList<>();
    private final List<StructurePointBuilder> points = new ArrayList<>();
    private final List<StructureComponent> components = new ArrayList<>();
    private final List<ContextRuleDescriptor> contextRules = new ArrayList<>();
    private final List<StructureVersionDescriptor> versions = new ArrayList<>();
    private final List<MigrationDescriptor> migrations = new ArrayList<>();

    public StructureDefinitionBuilder(ResourceLocation id) {
        if (id == null) {
            throw new IllegalArgumentException("Structure id must not be null");
        }
        this.id = id;
    }

    public StructureDefinitionBuilder transforms(TransformDescriptor... transforms) {
        this.transforms.addAll(Arrays.asList(transforms));
        return this;
    }

    public StructureDefinitionBuilder point(String id, RelativeBlockPos location, TargetPattern target) {
        points.add(new StructurePointBuilder(id, location, target));
        return this;
    }

    public StructureDefinitionBuilder component(StructureComponent component) {
        if (component == null) {
            throw new IllegalArgumentException("Structure component must not be null");
        }
        components.add(component);
        return this;
    }

    public StructureDefinitionBuilder context(ContextRuleDescriptor rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Context rule must not be null");
        }
        contextRules.add(rule);
        return this;
    }

    public StructureDefinitionBuilder version(StructureVersionDescriptor version) {
        if (version == null) {
            throw new IllegalArgumentException("Structure version must not be null");
        }
        versions.add(version);
        return this;
    }

    public StructureDefinitionBuilder migration(MigrationDescriptor migration) {
        if (migration == null) {
            throw new IllegalArgumentException("Structure migration must not be null");
        }
        migrations.add(migration);
        return this;
    }

    public StructureDefinition build() {
        return StructureDefinitionCompiler.compile(this);
    }

    public ResourceLocation id() {
        return id;
    }

    public List<TransformDescriptor> transforms() {
        return List.copyOf(transforms);
    }

    public List<StructurePointBuilder> points() {
        return List.copyOf(points);
    }

    public List<StructureComponent> components() {
        return List.copyOf(components);
    }

    public List<ContextRuleDescriptor> contextRules() {
        return List.copyOf(contextRules);
    }

    public List<StructureVersionDescriptor> versions() {
        return List.copyOf(versions);
    }

    public List<MigrationDescriptor> migrations() {
        return List.copyOf(migrations);
    }
}
