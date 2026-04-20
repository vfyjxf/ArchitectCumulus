package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.component.StructureComponent;
import dev.vfyjxf.cumulus.api.target.TargetPattern;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import dev.vfyjxf.cumulus.impl.definition.StructureDefinitionCompiler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class StructureDefinitionBuilder {
    private final ResourceLocation id;
    private final List<TransformDescriptor> transforms = new ArrayList<>();
    private final List<StructurePointBuilder> points = new ArrayList<>();
    private final List<StructureComponent> components = new ArrayList<>();

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
}
