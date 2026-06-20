package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class RuntimeAssetReloadListener extends SimplePreparableReloadListener<RuntimeAssetLoadResult> {
    public static final String rootPath = "cumulus/structures";

    private final RuntimeAssetRepository repository;
    private final Runnable validReloadHook;
    private volatile RuntimeAssetLoadResult lastResult;

    public RuntimeAssetReloadListener() {
        this(new RuntimeAssetRepository());
    }

    public RuntimeAssetReloadListener(RuntimeAssetRepository repository) {
        this(repository, () -> {
        });
    }

    public RuntimeAssetReloadListener(RuntimeAssetRepository repository, Runnable validReloadHook) {
        if (repository == null) {
            throw new IllegalArgumentException("Runtime asset repository must not be null");
        }
        if (validReloadHook == null) {
            throw new IllegalArgumentException("Runtime asset valid reload hook must not be null");
        }
        this.repository = repository;
        this.validReloadHook = validReloadHook;
    }

    @Override
    protected RuntimeAssetLoadResult prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return loadFrom(resourceManager);
    }

    @Override
    public void apply(RuntimeAssetLoadResult result, ResourceManager resourceManager, ProfilerFiller profiler) {
        lastResult = result;
        repository.apply(result);
        if (result.valid()) {
            validReloadHook.run();
        }
    }

    public RuntimeAssetLoadResult loadFrom(ResourceManager resourceManager) {
        var sources = new ArrayList<RuntimeAssetJsonSource>();
        var diagnostics = new ArrayList<DiagnosticNode>();
        var edges = new ArrayList<DiagnosticEdge>();
        resourceManager.listResources(rootPath, RuntimeAssetReloadListener::isRuntimeAsset).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    try (var reader = entry.getValue().openAsReader()) {
                        sources.add(new RuntimeAssetJsonSource(resourcePath(entry.getKey()), reader.lines()
                                .collect(java.util.stream.Collectors.joining("\n"))));
                    } catch (IOException ex) {
                        addResourceIoDiagnostic(diagnostics, edges, resourcePath(entry.getKey()));
                    }
                });
        if (!diagnostics.isEmpty()) {
            return new RuntimeAssetLoadResult(
                    List.of(),
                    new DiagnosticGraph("root", diagnostics, edges));
        }
        return new RuntimeAssetJsonLoader().load(sources);
    }

    public Optional<RuntimeAssetLoadResult> lastResult() {
        return Optional.ofNullable(lastResult);
    }

    public RuntimeAssetRepository repository() {
        return repository;
    }

    private static boolean isRuntimeAsset(ResourceLocation location) {
        return location.getPath().startsWith(rootPath + "/")
                && location.getPath().endsWith(".json");
    }

    private static String resourcePath(ResourceLocation location) {
        return "data/" + location.getNamespace() + "/" + location.getPath();
    }

    private static void addResourceIoDiagnostic(
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges,
            String sourcePath) {
        if (diagnostics.isEmpty()) {
            diagnostics.add(new DiagnosticNode(
                    "root",
                    Severity.info,
                    "Runtime asset datapack loading failed",
                    "diagnostic.cumulus.asset_reload_failed",
                    List.of(),
                    "runtime/assets/reload",
                    List.of()));
        }
        diagnostics.add(new DiagnosticNode(
                "INVALID_RESOURCE_IO",
                Severity.error,
                "Failed to read runtime asset resource",
                "diagnostic.cumulus.invalid_resource_io",
                List.of(),
                sourcePath,
                List.of()));
        edges.add(new DiagnosticEdge("root", "INVALID_RESOURCE_IO", "caused_by"));
    }
}
