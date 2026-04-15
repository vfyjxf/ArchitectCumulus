package dev.vfyjxf.cumulus.api.dsl;

import dev.vfyjxf.cumulus.api.ArchitectSchema;
import dev.vfyjxf.cumulus.api.binding.plan.BindingPlan;
import dev.vfyjxf.cumulus.common.binder.PlanBinder;
import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.pass.Binder;
import dev.vfyjxf.cumulus.api.pass.Checker;
import dev.vfyjxf.cumulus.api.pass.Compiler;
import dev.vfyjxf.cumulus.api.pass.DependencyDeclarer;
import dev.vfyjxf.cumulus.api.pass.Exporter;
import dev.vfyjxf.cumulus.api.pass.Linker;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 代码侧 schema DSL。
 *
 * <p>目标：用流式 API 构建 {@link dev.vfyjxf.cumulus.api.ArchitectSchema}，并保持与数据驱动 schema 的结构同构。
 *
 * <p>示例：
 * <pre>{@code
 * ArchitectSchema schema = ArchitectSchemaDsl.schema(id)
 *     .anchor(a -> a.finder(anchorFinder))
 *     .bind(b -> b.plan(planDsl -> planDsl.role("controller").fromAnchorBlock()))
 *     .check("rule1", c -> c.checker(checker))
 *     .compile(c -> c.compiler(compiler))
 *     .build();
 * }</pre>
 */
public final class ArchitectSchemaDsl {
    private ArchitectSchemaDsl() {
    }

    public static SchemaBuilder schema(ResourceLocation id) {
        return new SchemaBuilder(ArchitectSchema.builder(id));
    }

    public static final class SchemaBuilder {
        private final ArchitectSchema.Builder delegate;

        private SchemaBuilder(ArchitectSchema.Builder delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public SchemaBuilder anchor(Consumer<AnchorStage> config) {
            Objects.requireNonNull(config, "config").accept(new AnchorStage(delegate));
            return this;
        }

        public SchemaBuilder bind(Consumer<BindStage> config) {
            Objects.requireNonNull(config, "config").accept(new BindStage(delegate));
            return this;
        }

        public SchemaBuilder check(String id, Consumer<CheckerStage> config) {
            Objects.requireNonNull(config, "config").accept(new CheckerStage(delegate, id));
            return this;
        }

        public SchemaBuilder compile(Consumer<CompileStage> config) {
            Objects.requireNonNull(config, "config").accept(new CompileStage(delegate));
            return this;
        }

        public SchemaBuilder depends(Consumer<DependsStage> config) {
            Objects.requireNonNull(config, "config").accept(new DependsStage(delegate));
            return this;
        }

        public SchemaBuilder link(Consumer<LinkStage> config) {
            Objects.requireNonNull(config, "config").accept(new LinkStage(delegate));
            return this;
        }

        public SchemaBuilder export(Consumer<ExportStage> config) {
            Objects.requireNonNull(config, "config").accept(new ExportStage(delegate));
            return this;
        }

        public ArchitectSchema build() {
            return delegate.build();
        }
    }

    public static final class AnchorStage {
        private final ArchitectSchema.Builder delegate;

        private AnchorStage(ArchitectSchema.Builder delegate) {
            this.delegate = delegate;
        }

        public void finder(AnchorFinder finder) {
            delegate.anchorFinder(finder);
        }
    }

    public static final class BindStage {
        private final ArchitectSchema.Builder delegate;

        private BindStage(ArchitectSchema.Builder delegate) {
            this.delegate = delegate;
        }

        public void binder(Binder binder) {
            delegate.binder(binder);
        }

        /**
         * 以 DSL 构建 {@link BindingPlan}，并使用 common {@code PlanBinder} 执行。
         */
        public void plan(java.util.function.Consumer<BindingPlanDsl> config) {
            BindingPlanDsl dsl = new BindingPlanDsl();
            Objects.requireNonNull(config, "config").accept(dsl);
            delegate.binder(PlanBinder.of(dsl.build()));
        }

        /**
         * 直接使用一个 {@link BindingPlan}，并使用 common {@code PlanBinder} 执行。
         */
        public void plan(BindingPlan plan) {
            delegate.binder(PlanBinder.of(Objects.requireNonNull(plan, "plan")));
        }
    }

    public static final class CheckerStage {
        private final ArchitectSchema.Builder delegate;
        private final String id;

        private CheckerStage(ArchitectSchema.Builder delegate, String id) {
            this.delegate = delegate;
            this.id = Objects.requireNonNull(id, "id");
        }

        public void checker(Checker checker) {
            delegate.addChecker(id, checker);
        }
    }

    public static final class CompileStage {
        private final ArchitectSchema.Builder delegate;

        private CompileStage(ArchitectSchema.Builder delegate) {
            this.delegate = delegate;
        }

        public void compiler(Compiler compiler) {
            delegate.compiler(compiler);
        }
    }

    public static final class DependsStage {
        private final ArchitectSchema.Builder delegate;

        private DependsStage(ArchitectSchema.Builder delegate) {
            this.delegate = delegate;
        }

        public void declarer(DependencyDeclarer dependencyDeclarer) {
            delegate.dependencyDeclarer(dependencyDeclarer);
        }
    }

    public static final class LinkStage {
        private final ArchitectSchema.Builder delegate;

        private LinkStage(ArchitectSchema.Builder delegate) {
            this.delegate = delegate;
        }

        public void linker(Linker linker) {
            delegate.linker(linker);
        }
    }

    public static final class ExportStage {
        private final ArchitectSchema.Builder delegate;

        private ExportStage(ArchitectSchema.Builder delegate) {
            this.delegate = delegate;
        }

        public void exporter(Exporter exporter) {
            delegate.exporter(exporter);
        }
    }
}
