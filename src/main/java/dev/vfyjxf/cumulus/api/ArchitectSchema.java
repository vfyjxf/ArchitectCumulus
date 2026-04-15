package dev.vfyjxf.cumulus.api;

import dev.vfyjxf.cumulus.api.pass.AnchorFinder;
import dev.vfyjxf.cumulus.api.pass.Binder;
import dev.vfyjxf.cumulus.api.pass.Checker;
import dev.vfyjxf.cumulus.api.pass.Compiler;
import dev.vfyjxf.cumulus.api.pass.DependencyDeclarer;
import dev.vfyjxf.cumulus.api.pass.Exporter;
import dev.vfyjxf.cumulus.api.pass.Linker;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 一个结构类型（“一台机器”）的完整流水线定义。
 *
 * <p>一个 {@code ArchitectSchema} 只描述“如何识别/绑定/检查/编译/依赖/链接/导出”某类结构，
 * 引擎会在每 tick 以 {@link dev.vfyjxf.cumulus.api.engine.InstanceKey}（schemaId + anchorPos）为单位运行这些 pass。
 *
 * <p>典型约束与约定：
 * <ul>
 *     <li>anchorFinder 只在已加载范围内 discover/probe，不强制加载区块</li>
 *     <li>binder 负责扫描世界并输出 bindings，后续 pass 尽量只读 bindings 而不是重复扫描</li>
 *     <li>checkers 可以有多个，按声明顺序执行；任一 checker 失败可使实例进入 ERROR/INCOMPLETE</li>
 *     <li>compiler 将 bindings 编译为运行时上下文（{@code RuntimeContext}）供机器逻辑消费</li>
 *     <li>dependencyDeclarer/linker/exporter 为可选阶段，默认实现为 no-op</li>
 * </ul>
 *
 * <p>线程与性能：建议 schema 组件尽量无状态/可复用；如果需要缓存，请将缓存放在引擎侧或实例侧，
 * 并通过 {@link dev.vfyjxf.cumulus.api.engine.Budget} 控制每 tick 的总成本。
 */
public record ArchitectSchema(
        ResourceLocation id,
        AnchorFinder anchorFinder,
        Binder binder,
        List<ArchitectSchema.NamedChecker> checkers,
        Compiler compiler,
        DependencyDeclarer dependencyDeclarer,
        Linker linker,
        Exporter exporter
) {
    public ArchitectSchema {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(anchorFinder, "anchorFinder");
        Objects.requireNonNull(binder, "binder");
        checkers = List.copyOf(Objects.requireNonNull(checkers, "checkers"));
        Objects.requireNonNull(compiler, "compiler");
        Objects.requireNonNull(dependencyDeclarer, "dependencyDeclarer");
        Objects.requireNonNull(linker, "linker");
        Objects.requireNonNull(exporter, "exporter");
    }

    public Optional<Checker> checker(String id) {
        Objects.requireNonNull(id, "id");
        return checkers.stream()
                .filter(c -> id.equals(c.id()))
                .map(NamedChecker::checker)
                .findFirst();
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public record NamedChecker(String id, Checker checker) {
        public NamedChecker {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(checker, "checker");
        }
    }

    public static final class Builder {
        private final ResourceLocation id;
        private AnchorFinder anchorFinder;
        private Binder binder;
        private final List<NamedChecker> checkers = new ArrayList<>();
        private Compiler compiler;
        private DependencyDeclarer dependencyDeclarer = DependencyDeclarer.none();
        private Linker linker = Linker.none();
        private Exporter exporter = Exporter.none();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * 设置锚点发现器（只在已加载范围内 discover/probe）。
         */
        public Builder anchorFinder(AnchorFinder anchorFinder) {
            this.anchorFinder = Objects.requireNonNull(anchorFinder, "anchorFinder");
            return this;
        }

        /**
         * 设置 binder（负责扫描世界并输出 bindings）。
         */
        public Builder binder(Binder binder) {
            this.binder = Objects.requireNonNull(binder, "binder");
            return this;
        }

        /**
         * 添加一个命名 checker。
         *
         * <p>checker 会按添加顺序执行；id 用于诊断/调试与数据驱动映射。
         */
        public Builder addChecker(String id, Checker checker) {
            checkers.add(new NamedChecker(id, checker));
            return this;
        }

        /**
         * 设置 compiler（把结构编译为运行时上下文）。
         */
        public Builder compiler(Compiler compiler) {
            this.compiler = Objects.requireNonNull(compiler, "compiler");
            return this;
        }

        /**
         * 设置依赖声明器（可选阶段）。
         */
        public Builder dependencyDeclarer(DependencyDeclarer dependencyDeclarer) {
            this.dependencyDeclarer = Objects.requireNonNull(dependencyDeclarer, "dependencyDeclarer");
            return this;
        }

        /**
         * 设置 linker（可选阶段；默认 {@link Linker#none()}）。
         */
        public Builder linker(Linker linker) {
            this.linker = Objects.requireNonNull(linker, "linker");
            return this;
        }

        /**
         * 设置 exporter（可选阶段；默认 {@link Exporter#none()}）。
         */
        public Builder exporter(Exporter exporter) {
            this.exporter = Objects.requireNonNull(exporter, "exporter");
            return this;
        }

        /**
         * 构建 schema。
         *
         * @throws NullPointerException 当必需组件缺失时抛出（anchorFinder/binder/compiler）
         */
        public ArchitectSchema build() {
            return new ArchitectSchema(
                    id,
                    Objects.requireNonNull(anchorFinder, "anchorFinder"),
                    Objects.requireNonNull(binder, "binder"),
                    checkers,
                    Objects.requireNonNull(compiler, "compiler"),
                    dependencyDeclarer,
                    linker,
                    exporter
            );
        }
    }
}
