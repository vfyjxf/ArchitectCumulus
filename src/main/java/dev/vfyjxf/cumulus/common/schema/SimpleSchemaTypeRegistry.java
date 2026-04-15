package dev.vfyjxf.cumulus.common.schema;

import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.pass.Binder;
import dev.vfyjxf.cumulus.api.pass.Checker;
import dev.vfyjxf.cumulus.api.pass.Compiler;
import dev.vfyjxf.cumulus.api.pass.DependencyDeclarer;
import dev.vfyjxf.cumulus.api.pass.Exporter;
import dev.vfyjxf.cumulus.api.pass.Linker;
import dev.vfyjxf.cumulus.api.schema.SchemaComponentType;
import dev.vfyjxf.cumulus.api.schema.SchemaTypeRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 一个简单的内存 {@link SchemaTypeRegistry} 实现（common）。
 *
 * <p>用于把数据驱动的 {@code typeId -> SchemaComponentType} 映射落地。
 */
public final class SimpleSchemaTypeRegistry implements SchemaTypeRegistry {
    private final Map<ResourceLocation, SchemaComponentType<AnchorFinder>> anchorFinders = new HashMap<>();
    private final Map<ResourceLocation, SchemaComponentType<Binder>> binders = new HashMap<>();
    private final Map<ResourceLocation, SchemaComponentType<Checker>> checkers = new HashMap<>();
    private final Map<ResourceLocation, SchemaComponentType<Compiler>> compilers = new HashMap<>();
    private final Map<ResourceLocation, SchemaComponentType<DependencyDeclarer>> dependencyDeclarers = new HashMap<>();
    private final Map<ResourceLocation, SchemaComponentType<Linker>> linkers = new HashMap<>();
    private final Map<ResourceLocation, SchemaComponentType<Exporter>> exporters = new HashMap<>();

    public SimpleSchemaTypeRegistry registerAnchorFinder(SchemaComponentType<AnchorFinder> type) {
        anchorFinders.put(Objects.requireNonNull(type, "type").id(), type);
        return this;
    }

    public SimpleSchemaTypeRegistry registerBinder(SchemaComponentType<Binder> type) {
        binders.put(Objects.requireNonNull(type, "type").id(), type);
        return this;
    }

    public SimpleSchemaTypeRegistry registerChecker(SchemaComponentType<Checker> type) {
        checkers.put(Objects.requireNonNull(type, "type").id(), type);
        return this;
    }

    public SimpleSchemaTypeRegistry registerCompiler(SchemaComponentType<Compiler> type) {
        compilers.put(Objects.requireNonNull(type, "type").id(), type);
        return this;
    }

    public SimpleSchemaTypeRegistry registerDependencyDeclarer(SchemaComponentType<DependencyDeclarer> type) {
        dependencyDeclarers.put(Objects.requireNonNull(type, "type").id(), type);
        return this;
    }

    public SimpleSchemaTypeRegistry registerLinker(SchemaComponentType<Linker> type) {
        linkers.put(Objects.requireNonNull(type, "type").id(), type);
        return this;
    }

    public SimpleSchemaTypeRegistry registerExporter(SchemaComponentType<Exporter> type) {
        exporters.put(Objects.requireNonNull(type, "type").id(), type);
        return this;
    }

    @Override
    public Optional<SchemaComponentType<AnchorFinder>> anchorFinder(ResourceLocation type) {
        return Optional.ofNullable(anchorFinders.get(type));
    }

    @Override
    public Optional<SchemaComponentType<Binder>> binder(ResourceLocation type) {
        return Optional.ofNullable(binders.get(type));
    }

    @Override
    public Optional<SchemaComponentType<Checker>> checker(ResourceLocation type) {
        return Optional.ofNullable(checkers.get(type));
    }

    @Override
    public Optional<SchemaComponentType<Compiler>> compiler(ResourceLocation type) {
        return Optional.ofNullable(compilers.get(type));
    }

    @Override
    public Optional<SchemaComponentType<DependencyDeclarer>> dependencyDeclarer(ResourceLocation type) {
        return Optional.ofNullable(dependencyDeclarers.get(type));
    }

    @Override
    public Optional<SchemaComponentType<Linker>> linker(ResourceLocation type) {
        return Optional.ofNullable(linkers.get(type));
    }

    @Override
    public Optional<SchemaComponentType<Exporter>> exporter(ResourceLocation type) {
        return Optional.ofNullable(exporters.get(type));
    }
}
