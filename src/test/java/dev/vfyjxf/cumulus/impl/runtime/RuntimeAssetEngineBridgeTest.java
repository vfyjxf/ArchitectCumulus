package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.datagen.DataDrivenStructureAsset;
import dev.vfyjxf.cumulus.api.types.ContextKey;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinitionRegistry;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.impl.RuntimeAssetExporter;
import dev.vfyjxf.cumulus.impl.StructureRuntimePlanCompiler;
import dev.vfyjxf.cumulus.impl.check.SnapshotTarget;
import dev.vfyjxf.cumulus.impl.check.TargetSnapshotSource;
import dev.vfyjxf.cumulus.impl.codec.RuntimeAssetJsonLoader;
import dev.vfyjxf.cumulus.impl.codec.RuntimeAssetJsonSource;
import dev.vfyjxf.cumulus.impl.codec.RuntimeAssetRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeAssetEngineBridgeTest {
    private static final ResourceLocation block = ResourceLocation.parse("cumulus:block");

    @Test
    void registersEmbeddedDefinitionsFromLatestValidRuntimeAssetSnapshot() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:bridged_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller"))
                        .capture("controller"))
                .build();
        var exported = RuntimeAssetExporter.export(
                DataDrivenStructureAsset.fromDefinition(
                        ResourceLocation.parse("cumulus:bridged_machine_asset"),
                        1,
                        definition,
                        false),
                StructureRuntimePlanCompiler.compile(definition));
        var repository = new RuntimeAssetRepository();
        repository.apply(new RuntimeAssetJsonLoader().load(List.of(new RuntimeAssetJsonSource(
                exported.resourcePath(),
                exported.serialized()))));
        var engine = new RuntimeCheckEngine();

        var registered = new RuntimeAssetEngineBridge(repository, engine).registerDefinitions();

        assertEquals(List.of(definition.id()), registered);
        engine.track(definition.id(), new BlockPos(0, 64, 0), source()
                .put(block, RelativeBlockPos.of(0, 0, 0), "cumulus:controller"));
        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(10, 4);
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
        assertEquals("0,64,0", frame.deltas().getFirst().context().get(ContextKey.string("controller")).orElse(null));
    }

    @Test
    void registersIdOnlyAssetsFromExternalDefinitionRegistry() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:id_only"))
                .point("origin", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();
        var repository = new RuntimeAssetRepository();
        repository.apply(new RuntimeAssetJsonLoader().load(List.of(new RuntimeAssetJsonSource(
                "data/cumulus/cumulus/structures/id_only.json",
                """
                        {
                          "id": "cumulus:id_only",
                          "schema_version": 1,
                          "definition": "cumulus:id_only",
                          "debug_asset": false,
                          "runtime_plan": {
                            "definition": "cumulus:id_only",
                            "entries": [
                              {
                                "id": "origin",
                                "target": "cumulus:block",
                                "min": "0,0,0",
                                "max": "0,0,0"
                              }
                            ]
                          }
                        }
                        """))));

        var engine = new RuntimeCheckEngine();
        var registered = new RuntimeAssetEngineBridge(repository, engine, id ->
                id.equals(definition.id()) ? Optional.of(definition) : Optional.empty())
                .registerDefinitions();

        assertEquals(List.of(definition.id()), registered);
        engine.track(definition.id(), new BlockPos(0, 64, 0), source()
                .put(block, RelativeBlockPos.of(0, 0, 0), "cumulus:controller"));
        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(10, 4);
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
    }

    @Test
    void registersIdOnlyAssetsFromStructureDefinitionRegistry() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:id_only_registry"))
                .point("origin", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();
        var registry = new StructureDefinitionRegistry().register(definition);
        var repository = new RuntimeAssetRepository();
        repository.apply(new RuntimeAssetJsonLoader().load(List.of(new RuntimeAssetJsonSource(
                "data/cumulus/cumulus/structures/id_only_registry.json",
                """
                        {
                          "id": "cumulus:id_only_registry",
                          "schema_version": 1,
                          "definition": "cumulus:id_only_registry",
                          "debug_asset": false,
                          "runtime_plan": {
                            "definition": "cumulus:id_only_registry",
                            "entries": [
                              {
                                "id": "origin",
                                "target": "cumulus:block",
                                "min": "0,0,0",
                                "max": "0,0,0"
                              }
                            ]
                          }
                        }
                        """))));
        var engine = new RuntimeCheckEngine();

        var registered = new RuntimeAssetEngineBridge(repository, engine, registry).registerDefinitions();

        assertEquals(List.of(definition.id()), registered);
        engine.track(definition.id(), new BlockPos(0, 64, 0), source()
                .put(block, RelativeBlockPos.of(0, 0, 0), "cumulus:controller"));
        engine.markDirty(new SectionKey(0, 4, 0));
        var frame = engine.tick(10, 4);
        assertEquals(InstanceStatus.valid, frame.deltas().getFirst().status());
    }

    @Test
    void skipsIdOnlyAssetsWhenExternalRegistryCannotResolveDefinition() {
        var repository = new RuntimeAssetRepository();
        repository.apply(new RuntimeAssetJsonLoader().load(List.of(new RuntimeAssetJsonSource(
                "data/cumulus/cumulus/structures/id_only.json",
                """
                        {
                          "id": "cumulus:id_only",
                          "schema_version": 1,
                          "definition": "cumulus:id_only",
                          "debug_asset": false,
                          "runtime_plan": {
                            "definition": "cumulus:id_only",
                            "entries": [
                              {
                                "id": "origin",
                                "target": "cumulus:block",
                                "min": "0,0,0",
                                "max": "0,0,0"
                              }
                            ]
                          }
                        }
                        """))));

        var registered = new RuntimeAssetEngineBridge(repository, new RuntimeCheckEngine()).registerDefinitions();

        assertEquals(List.of(), registered);
    }

    private static BuilderSource source() {
        return new BuilderSource();
    }

    private static final class BuilderSource implements TargetSnapshotSource {
        private final Map<Key, SnapshotTarget> targets = new LinkedHashMap<>();

        BuilderSource put(ResourceLocation targetType, RelativeBlockPos pos, String value) {
            targets.put(new Key(targetType, pos, pos), new SnapshotTarget(value, Map.of()));
            return this;
        }

        @Override
        public Optional<SnapshotTarget> target(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max) {
            return Optional.ofNullable(targets.get(new Key(targetType, min, max)));
        }

        @Override
        public Optional<String> context(String key) {
            return Optional.empty();
        }

        private record Key(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max) {
        }
    }
}
