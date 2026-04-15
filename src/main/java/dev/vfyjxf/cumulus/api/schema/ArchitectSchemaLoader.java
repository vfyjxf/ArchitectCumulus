package dev.vfyjxf.cumulus.api.schema;

import dev.vfyjxf.cumulus.api.ArchitectSchema;
import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.pass.Binder;
import dev.vfyjxf.cumulus.api.pass.Checker;
import dev.vfyjxf.cumulus.api.pass.Compiler;
import dev.vfyjxf.cumulus.api.pass.DependencyDeclarer;
import dev.vfyjxf.cumulus.api.pass.Exporter;
import dev.vfyjxf.cumulus.api.pass.Linker;

import java.util.Objects;

/**
 * 将 {@link ArchitectSchemaSpec} 加载为运行时 {@link dev.vfyjxf.cumulus.api.ArchitectSchema}。
 *
 * <p>loader 只负责按 spec 组装组件，不做额外语义校验（由各组件自行校验 config 并抛错）。
 */
public final class ArchitectSchemaLoader {
    private ArchitectSchemaLoader() {
    }

    /**
     * 加载一个 schema。
     *
     * @throws IllegalArgumentException 当 type 未注册或 config 非法时抛出
     */
    public static ArchitectSchema load(ArchitectSchemaSpec spec, SchemaTypeRegistry registry, SchemaBuildContext ctx) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(ctx, "ctx");

        AnchorFinder anchorFinder = registry.anchorFinder(spec.anchorFinder().type())
                .orElseThrow(() -> new IllegalArgumentException("Unknown anchor_finder type: " + spec.anchorFinder().type()))
                .create(spec.anchorFinder().config(), ctx);
        Binder binder = registry.binder(spec.binder().type())
                .orElseThrow(() -> new IllegalArgumentException("Unknown binder type: " + spec.binder().type()))
                .create(spec.binder().config(), ctx);
        Compiler compiler = registry.compiler(spec.compiler().type())
                .orElseThrow(() -> new IllegalArgumentException("Unknown compiler type: " + spec.compiler().type()))
                .create(spec.compiler().config(), ctx);

        DependencyDeclarer dependencyDeclarer = spec.dependencyDeclarer()
                .map(s -> registry.dependencyDeclarer(s.type())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown dependency_declarer type: " + s.type()))
                        .create(s.config(), ctx))
                .orElseGet(DependencyDeclarer::none);

        Linker linker = spec.linker()
                .map(s -> registry.linker(s.type())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown linker type: " + s.type()))
                        .create(s.config(), ctx))
                .orElseGet(Linker::none);

        Exporter exporter = spec.exporter()
                .map(s -> registry.exporter(s.type())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown exporter type: " + s.type()))
                        .create(s.config(), ctx))
                .orElseGet(Exporter::none);

        ArchitectSchema.Builder builder = ArchitectSchema.builder(spec.id())
                .anchorFinder(anchorFinder)
                .binder(binder)
                .compiler(compiler)
                .dependencyDeclarer(dependencyDeclarer)
                .linker(linker)
                .exporter(exporter);

        for (CheckerSpec checker : spec.checkers()) {
            Checker impl = registry.checker(checker.component().type())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown checker type: " + checker.component().type()))
                    .create(checker.component().config(), ctx);
            builder.addChecker(checker.id(), impl);
        }

        return builder.build();
    }
}
