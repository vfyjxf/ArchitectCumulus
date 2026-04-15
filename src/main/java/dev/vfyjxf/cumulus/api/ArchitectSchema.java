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

        public Builder anchorFinder(AnchorFinder anchorFinder) {
            this.anchorFinder = Objects.requireNonNull(anchorFinder, "anchorFinder");
            return this;
        }

        public Builder binder(Binder binder) {
            this.binder = Objects.requireNonNull(binder, "binder");
            return this;
        }

        public Builder addChecker(String id, Checker checker) {
            checkers.add(new NamedChecker(id, checker));
            return this;
        }

        public Builder compiler(Compiler compiler) {
            this.compiler = Objects.requireNonNull(compiler, "compiler");
            return this;
        }

        public Builder dependencyDeclarer(DependencyDeclarer dependencyDeclarer) {
            this.dependencyDeclarer = Objects.requireNonNull(dependencyDeclarer, "dependencyDeclarer");
            return this;
        }

        public Builder linker(Linker linker) {
            this.linker = Objects.requireNonNull(linker, "linker");
            return this;
        }

        public Builder exporter(Exporter exporter) {
            this.exporter = Objects.requireNonNull(exporter, "exporter");
            return this;
        }

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
