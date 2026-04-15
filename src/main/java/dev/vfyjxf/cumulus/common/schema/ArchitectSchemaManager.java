package dev.vfyjxf.cumulus.common.schema;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.vfyjxf.cumulus.api.ArchitectSchema;
import dev.vfyjxf.cumulus.api.schema.ArchitectSchemaLoader;
import dev.vfyjxf.cumulus.api.schema.ArchitectSchemaSpec;
import dev.vfyjxf.cumulus.api.schema.SchemaBuildContext;
import dev.vfyjxf.cumulus.api.schema.SchemaTypeRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ArchitectSchemaManager {
    private final SchemaTypeRegistry registry;
    private final SchemaBuildContext buildContext;

    private volatile Map<ResourceLocation, ArchitectSchema> schemas = Map.of();
    private volatile Map<ResourceLocation, String> problems = Map.of();

    public ArchitectSchemaManager(SchemaTypeRegistry registry, SchemaBuildContext buildContext) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.buildContext = Objects.requireNonNull(buildContext, "buildContext");
    }

    public void reload(Map<ResourceLocation, JsonElement> jsons) {
        Objects.requireNonNull(jsons, "jsons");
        Map<ResourceLocation, ArchitectSchema> nextSchemas = new HashMap<>();
        Map<ResourceLocation, String> nextProblems = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            ResourceLocation id = entry.getKey();
            ArchitectSchemaSpec spec = ArchitectSchemaSpec.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(message -> nextProblems.put(id, message))
                    .orElse(null);
            if (spec == null) {
                continue;
            }
            try {
                ArchitectSchema schema = ArchitectSchemaLoader.load(spec, registry, buildContext);
                nextSchemas.put(schema.id(), schema);
            } catch (RuntimeException e) {
                nextProblems.put(id, Optional.ofNullable(e.getMessage()).orElse(e.getClass().getName()));
            }
        }

        this.schemas = Collections.unmodifiableMap(nextSchemas);
        this.problems = Collections.unmodifiableMap(nextProblems);
    }

    public Map<ResourceLocation, ArchitectSchema> schemas() {
        return schemas;
    }

    public Collection<ArchitectSchema> schemaValues() {
        return schemas.values();
    }

    public Map<ResourceLocation, String> problems() {
        return problems;
    }
}

