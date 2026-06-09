package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.datagen.DataDrivenStructureAsset;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.RuntimeAssetExporter;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeAssetReloadListenerTest {
    @Test
    void loadsRuntimeAssetsFromDatapackResources() {
        var exported = exportedAsset("cumulus:machines/reload_machine");
        var manager = new FakeResourceManager(Map.of(
                ResourceLocation.parse("cumulus:cumulus/structures/machines/reload_machine.json"),
                resource(exported.serialized()),
                ResourceLocation.parse("cumulus:cumulus/structures/readme.txt"),
                resource("{}"),
                ResourceLocation.parse("cumulus:other/reload_machine.json"),
                resource("{}")));
        var listener = new RuntimeAssetReloadListener();

        var result = listener.loadFrom(manager);

        assertTrue(result.valid());
        assertEquals(1, result.assets().size());
        assertEquals(ResourceLocation.parse("cumulus:machines/reload_machine"), result.assets().getFirst().id());
        assertEquals("data/cumulus/cumulus/structures/machines/reload_machine.json",
                result.assets().getFirst().resourcePath());
    }

    @Test
    void storesLastAppliedReloadResult() {
        var exported = exportedAsset("cumulus:machines/applied_machine");
        var manager = new FakeResourceManager(Map.of(
                ResourceLocation.parse("cumulus:cumulus/structures/machines/applied_machine.json"),
                resource(exported.serialized())));
        var listener = new RuntimeAssetReloadListener();

        listener.apply(listener.loadFrom(manager), manager, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);

        assertEquals(ResourceLocation.parse("cumulus:machines/applied_machine"),
                listener.lastResult().orElseThrow().assets().getFirst().id());
    }

    @Test
    void applyUpdatesRuntimeAssetRepository() {
        var exported = exportedAsset("cumulus:machines/repository_machine");
        var manager = new FakeResourceManager(Map.of(
                ResourceLocation.parse("cumulus:cumulus/structures/machines/repository_machine.json"),
                resource(exported.serialized())));
        var repository = new RuntimeAssetRepository();
        var listener = new RuntimeAssetReloadListener(repository);

        listener.apply(listener.loadFrom(manager), manager, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);

        assertEquals(ResourceLocation.parse("cumulus:machines/repository_machine"),
                repository.byId(ResourceLocation.parse("cumulus:machines/repository_machine")).orElseThrow().id());
    }

    @Test
    void validApplyRunsPostApplyHookAfterRepositoryUpdate() {
        var exported = exportedAsset("cumulus:machines/hooked_machine");
        var manager = new FakeResourceManager(Map.of(
                ResourceLocation.parse("cumulus:cumulus/structures/machines/hooked_machine.json"),
                resource(exported.serialized())));
        var repository = new RuntimeAssetRepository();
        var registered = new ArrayList<ResourceLocation>();
        var listener = new RuntimeAssetReloadListener(repository, () ->
                registered.add(repository.byId(ResourceLocation.parse("cumulus:machines/hooked_machine"))
                        .orElseThrow()
                        .id()));

        listener.apply(listener.loadFrom(manager), manager, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);

        assertEquals(List.of(ResourceLocation.parse("cumulus:machines/hooked_machine")), registered);
    }

    @Test
    void invalidApplyStoresDiagnosticsWithoutRunningPostApplyHook() {
        var manager = new FakeResourceManager(Map.of(
                ResourceLocation.parse("cumulus:cumulus/structures/broken.json"),
                resource("{\"id\":\"cumulus:broken\"")));
        var repository = new RuntimeAssetRepository();
        var registered = new ArrayList<ResourceLocation>();
        var listener = new RuntimeAssetReloadListener(repository, () ->
                registered.add(ResourceLocation.parse("cumulus:unexpected")));

        listener.apply(listener.loadFrom(manager), manager, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);

        assertTrue(repository.lastResult().orElseThrow().diagnostics().nodes().stream()
                .anyMatch(node -> node.severity() == Severity.ERROR));
        assertEquals(List.of(), registered);
    }

    @Test
    void resourceReadFailureProducesDiagnosticInsteadOfThrowing() {
        var manager = new FakeResourceManager(Map.of(
                ResourceLocation.parse("cumulus:cumulus/structures/broken.json"),
                new Resource(null, () -> {
                    throw new IOException("read failed");
                })));

        var result = new RuntimeAssetReloadListener().loadFrom(manager);

        assertEquals(List.of("INVALID_RESOURCE_IO"), result.diagnostics().nodes().stream()
                .filter(node -> node.severity() == Severity.ERROR)
                .map(node -> node.id())
                .toList());
    }

    private static dev.vfyjxf.cumulus.impl.ExportedRuntimeAsset exportedAsset(String id) {
        var definition = Structures.define(ResourceLocation.parse(id.replace("machines/", "")))
                .point("origin", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .build();
        return RuntimeAssetExporter.export(
                new DataDrivenStructureAsset(
                        ResourceLocation.parse(id),
                        1,
                        definition.id(),
                        false),
                StructureRuntimePlanCompiler.compile(definition));
    }

    private static Resource resource(String content) {
        return new Resource(
                null,
                () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private record FakeResourceManager(Map<ResourceLocation, Resource> resources) implements ResourceManager {
        @Override
        public Set<String> getNamespaces() {
            return Set.of("cumulus");
        }

        @Override
        public Optional<Resource> getResource(ResourceLocation location) {
            return Optional.ofNullable(resources.get(location));
        }

        @Override
        public List<Resource> getResourceStack(ResourceLocation location) {
            return getResource(location).stream().toList();
        }

        @Override
        public Map<ResourceLocation, Resource> listResources(
                String path,
                Predicate<ResourceLocation> filter) {
            return resources.entrySet().stream()
                    .filter(entry -> entry.getKey().getPath().startsWith(path))
                    .filter(entry -> filter.test(entry.getKey()))
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue));
        }

        @Override
        public Map<ResourceLocation, List<Resource>> listResourceStacks(
                String path,
                Predicate<ResourceLocation> filter) {
            return listResources(path, filter).entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> List.of(entry.getValue())));
        }

        @Override
        public Stream<PackResources> listPacks() {
            return Stream.of();
        }
    }
}
