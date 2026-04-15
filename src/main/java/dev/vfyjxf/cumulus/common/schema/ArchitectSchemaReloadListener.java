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

