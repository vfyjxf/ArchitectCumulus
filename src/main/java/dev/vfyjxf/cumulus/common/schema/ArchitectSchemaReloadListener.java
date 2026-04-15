package dev.vfyjxf.cumulus.common.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;
import java.util.Objects;

/**
 * 一个资源 reload listener 示例：从资源路径加载 schema JSON 并交给 {@link ArchitectSchemaManager}。
 *
 * <p>注意：本仓库当前主要提供 API/骨架；在真实 mod 中你需要把该 listener 注册到合适的 reload 管道。
 */
public final class ArchitectSchemaReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DEFAULT_PATH = "architect_cumulus/schemas";

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private final ArchitectSchemaManager manager;

    public ArchitectSchemaReloadListener(ArchitectSchemaManager manager) {
        this(manager, DEFAULT_PATH);
    }

    public ArchitectSchemaReloadListener(ArchitectSchemaManager manager, String path) {
        super(GSON, Objects.requireNonNull(path, "path"));
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        manager.reload(jsons);
    }
}
