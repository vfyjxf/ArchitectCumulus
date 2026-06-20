package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.component.StructureComponent;
import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.context.plan.CheckPhase;
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
    private CheckPhase checkPhase;
    private final List<StructurePointDefinition> shapePoints = new ArrayList<>();

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

    /**
     * Attach a {@link CheckPhase} — a validation hook that runs after static
     * point matching, can read matched data + write context, and return
     * additional points to match. Use {@code CheckPhases.repeatRange(...)} for
     * axial min/max repetition, or a custom lambda for arbitrary logic.
     */
    public StructureDefinitionBuilder checkPhase(CheckPhase phase) {
        if (phase == null) {
            throw new IllegalArgumentException("Check phase must not be null");
        }
        this.checkPhase = phase;
        return this;
    }

    // ── Shape DSL ─────────────────────────────────────────────────────

    /**
     * Define the structure's 3D layout via a {@link PartPattern} matrix indexed
     * as [y][z][x]. Uses {@link DefaultShapeEmitter} to expand each cell into a
     * {@link StructurePointDefinition}.
     */
    public StructureDefinitionBuilder shape(PartPattern[][][] shape) {
        return shape(shape, DefaultShapeEmitter.instance);
    }

    /**
     * Define the structure's 3D layout with a custom {@link ShapeEmitter}.
     */
    public StructureDefinitionBuilder shape(PartPattern[][][] shape, ShapeEmitter emitter) {
        emitter.emit(shape, new ShapeEmitterContext(shapePoints::add));
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

    /** The attached check phase, or null if none was set (no dynamic expansion). */
    public CheckPhase checkPhase() {
        return checkPhase;
    }

    /** Points emitted by {@link #shape}. Empty if no shape was used. */
    public List<StructurePointDefinition> shapePoints() {
        return List.copyOf(shapePoints);
    }
}
