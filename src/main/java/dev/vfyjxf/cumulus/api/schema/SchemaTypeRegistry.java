package dev.vfyjxf.cumulus.api.schema;

import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.pass.Binder;
import dev.vfyjxf.cumulus.api.pass.Checker;
import dev.vfyjxf.cumulus.api.pass.Compiler;
import dev.vfyjxf.cumulus.api.pass.DependencyDeclarer;
import dev.vfyjxf.cumulus.api.pass.Exporter;
import dev.vfyjxf.cumulus.api.pass.Linker;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * 数据驱动组件注册表：按 typeId 查找组件工厂。
 *
 * <p>v2 的 schema loader 会用该 registry 把 {@link ComponentSpec} 解析成运行时组件，
 * 从而构建 {@link dev.vfyjxf.cumulus.api.ArchitectSchema}。
 */
public interface SchemaTypeRegistry {
    Optional<SchemaComponentType<AnchorFinder>> anchorFinder(ResourceLocation type);

    Optional<SchemaComponentType<Binder>> binder(ResourceLocation type);

    Optional<SchemaComponentType<Checker>> checker(ResourceLocation type);

    Optional<SchemaComponentType<Compiler>> compiler(ResourceLocation type);

    Optional<SchemaComponentType<DependencyDeclarer>> dependencyDeclarer(ResourceLocation type);

    Optional<SchemaComponentType<Linker>> linker(ResourceLocation type);

    Optional<SchemaComponentType<Exporter>> exporter(ResourceLocation type);
}
