package dev.vfyjxf.cumulus.common.dummy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.vfyjxf.cumulus.api.ArchitectSchema;
import dev.vfyjxf.cumulus.api.binding.plan.BindingOp;
import dev.vfyjxf.cumulus.api.binding.plan.BindingPlan;
import dev.vfyjxf.cumulus.api.binding.plan.MatchSpec;
import dev.vfyjxf.cumulus.api.binding.plan.PartKind;
import dev.vfyjxf.cumulus.api.binding.plan.ScanSpec;
import dev.vfyjxf.cumulus.api.diagnostic.InstanceStatus;
import dev.vfyjxf.cumulus.api.engine.Budget;
import dev.vfyjxf.cumulus.api.engine.ChangeCause;
import dev.vfyjxf.cumulus.api.engine.ChangeSet;
import dev.vfyjxf.cumulus.api.engine.EngineFrame;
import dev.vfyjxf.cumulus.api.engine.InstanceDelta;
import dev.vfyjxf.cumulus.api.engine.InstanceView;
import dev.vfyjxf.cumulus.api.engine.TickContext;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.common.binder.PlanBinderType;
import dev.vfyjxf.cumulus.common.schema.ArchitectSchemaManager;
import dev.vfyjxf.cumulus.common.schema.BuiltInSchemaTypes;
import dev.vfyjxf.cumulus.common.schema.EmptySchemaBuildContext;
import dev.vfyjxf.cumulus.common.schema.SimpleSchemaTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
final class DummyArchitectEngineTest {
    private static final ResourceLocation TYPE_MATCH_BLOCK_ANCHOR_FINDER = ResourceLocation.fromNamespaceAndPath("cumulus", "match_block_anchor_finder");
    private static final ResourceLocation TYPE_ROLE_COUNT_CHECKER = ResourceLocation.fromNamespaceAndPath("cumulus", "role_count_checker");
    private static final ResourceLocation TYPE_BINDINGS_COMPILER = ResourceLocation.fromNamespaceAndPath("cumulus", "bindings_compiler");

    @Test
    void builtInSchemaTypesExposeExpectedTypes(MinecraftServer server) {
        SimpleSchemaTypeRegistry registry = new SimpleSchemaTypeRegistry();
        BuiltInSchemaTypes.registerAll(registry);

        assertTrue(registry.binder(PlanBinderType.ID).isPresent(), "plan_binder must be registered");
        assertTrue(registry.anchorFinder(TYPE_MATCH_BLOCK_ANCHOR_FINDER).isPresent(), "anchor_finder type must be registered");
        assertTrue(registry.checker(TYPE_ROLE_COUNT_CHECKER).isPresent(), "checker type must be registered");
        assertTrue(registry.compiler(TYPE_BINDINGS_COMPILER).isPresent(), "compiler type must be registered");
    }

    @Test
    void schemaManagerLoadsAndEngineTickRunsOk(MinecraftServer server) throws Exception {
        ResourceLocation schemaId = ResourceLocation.fromNamespaceAndPath("cumulus", "test_schema_ok");
        ResourceLocation controllerBlock = ResourceLocation.fromNamespaceAndPath("minecraft", "diamond_block");
        ResourceLocation blockEntityType = ResourceLocation.fromNamespaceAndPath("minecraft", "chest");

        BindingPlan plan = BindingPlan.builder()
                .add(BindingOp.matchId("controller", PartKind.BLOCK, new ScanSpec.Box(0, 0, 0, 0, 0, 0), controllerBlock))
                .add(BindingOp.matchId("be", PartKind.BLOCK_ENTITY, new ScanSpec.Box(1, 0, 0, 1, 0, 0), blockEntityType))
                .build();

        JsonElement spec = buildSchemaSpecJson(schemaId, controllerBlock, plan, true);

        SimpleSchemaTypeRegistry registry = new SimpleSchemaTypeRegistry();
        BuiltInSchemaTypes.registerAll(registry);

        ArchitectSchemaManager manager = new ArchitectSchemaManager(registry, EmptySchemaBuildContext.INSTANCE);
        manager.reload(Map.of(schemaId, spec));

        assertTrue(manager.problems().isEmpty(), "expected no schema problems, got: " + manager.problems());
        ArchitectSchema schema = manager.schemas().get(schemaId);
        assertNotNull(schema, "schema not loaded");

        InMemoryPartitionAccess world = new InMemoryPartitionAccess();
        BlockPos anchorPos = new BlockPos(0, 64, 0);
        world.loadSection(SectionKey.from(anchorPos));
        world.setBlock(anchorPos, controllerBlock);
        invokeSetBlockEntity(world, anchorPos.offset(1, 0, 0), blockEntityType);

        DummyArchitectEngine engine = new DummyArchitectEngine(world);
        engine.register(schema);
        engine.submit(ChangeSet.builder(ChangeCause.EVENT).dirtySection(SectionKey.from(anchorPos)).build());

        EngineFrame frame = engine.tick(new TickContext(0, Budget.unlimited()));
        assertFalse(frame.deltas().isEmpty(), "expected instance delta");
        assertTrue(frame.newRequests().isEmpty(), "expected no data requests, got: " + frame.newRequests());

        InstanceDelta delta = frame.deltas().getFirst();
        assertEquals(schemaId, delta.key().schemaId());
        assertEquals(anchorPos, delta.key().anchorPos());

        InstanceView view = delta.snapshot().orElseThrow();
        assertEquals(InstanceStatus.OK, view.status());
        assertTrue(view.runtimeContext().isPresent(), "expected compiled runtime context");
    }

    @Test
    void missingSectionsProduceIncompleteAndRequests(MinecraftServer server) {
        ResourceLocation schemaId = ResourceLocation.fromNamespaceAndPath("cumulus", "test_schema_incomplete");
        ResourceLocation controllerBlock = ResourceLocation.fromNamespaceAndPath("minecraft", "diamond_block");

        BindingPlan plan = BindingPlan.builder()
                .add(BindingOp.matchId("controller", PartKind.BLOCK, new ScanSpec.Box(0, 0, 0, 0, 0, 0), controllerBlock))
                .add(BindingOp.any("needs_far_block", PartKind.BLOCK, new ScanSpec.Box(16, 0, 0, 16, 0, 0)))
                .build();

        JsonElement spec = buildSchemaSpecJson(schemaId, controllerBlock, plan, false);

        SimpleSchemaTypeRegistry registry = new SimpleSchemaTypeRegistry();
        BuiltInSchemaTypes.registerAll(registry);

        ArchitectSchemaManager manager = new ArchitectSchemaManager(registry, EmptySchemaBuildContext.INSTANCE);
        manager.reload(Map.of(schemaId, spec));

        assertTrue(manager.problems().isEmpty(), "expected no schema problems, got: " + manager.problems());
        ArchitectSchema schema = manager.schemas().get(schemaId);
        assertNotNull(schema, "schema not loaded");

        InMemoryPartitionAccess world = new InMemoryPartitionAccess();
        BlockPos anchorPos = new BlockPos(0, 64, 0);
        world.loadSection(SectionKey.from(anchorPos));
        world.setBlock(anchorPos, controllerBlock);

        DummyArchitectEngine engine = new DummyArchitectEngine(world);
        engine.register(schema);
        engine.submit(ChangeSet.builder(ChangeCause.EVENT).dirtySection(SectionKey.from(anchorPos)).build());

        EngineFrame frame = engine.tick(new TickContext(0, Budget.unlimited()));
        assertFalse(frame.deltas().isEmpty(), "expected instance delta");
        InstanceView view = frame.deltas().getFirst().snapshot().orElseThrow();
        assertEquals(InstanceStatus.INCOMPLETE, view.status());
        assertFalse(frame.newRequests().isEmpty(), "expected data requests");
    }

    private static JsonElement buildSchemaSpecJson(ResourceLocation id, ResourceLocation anchorBlock, BindingPlan plan, boolean includeCheckerAndCompiler) {
        JsonElement anchorConfig = MatchSpec.CODEC.encodeStart(JsonOps.INSTANCE, MatchSpec.id(anchorBlock)).getOrThrow();
        JsonElement binderConfig = BindingPlan.CODEC.encodeStart(JsonOps.INSTANCE, plan).getOrThrow();

        JsonObject root = new JsonObject();
        root.addProperty("id", id.toString());

        JsonObject anchorFinder = new JsonObject();
        anchorFinder.addProperty("type", TYPE_MATCH_BLOCK_ANCHOR_FINDER.toString());
        anchorFinder.add("config", anchorConfig);
        root.add("anchor_finder", anchorFinder);

        JsonObject binder = new JsonObject();
        binder.addProperty("type", PlanBinderType.ID.toString());
        binder.add("config", binderConfig);
        root.add("binder", binder);

        if (includeCheckerAndCompiler) {
            JsonObject checkerConfig = new JsonObject();
            checkerConfig.addProperty("role", "controller");
            checkerConfig.addProperty("min", 1);
            checkerConfig.addProperty("max", 1);

            JsonObject checkerComponent = new JsonObject();
            checkerComponent.addProperty("type", TYPE_ROLE_COUNT_CHECKER.toString());
            checkerComponent.add("config", checkerConfig);

            JsonObject checkerEntry = new JsonObject();
            checkerEntry.addProperty("id", "controller_required");
            checkerEntry.add("component", checkerComponent);

            com.google.gson.JsonArray checkers = new com.google.gson.JsonArray();
            checkers.add(checkerEntry);
            root.add("checkers", checkers);

            JsonObject compiler = new JsonObject();
            compiler.addProperty("type", TYPE_BINDINGS_COMPILER.toString());
            compiler.add("config", new JsonObject());
            root.add("compiler", compiler);
        } else {
            JsonObject compiler = new JsonObject();
            compiler.addProperty("type", TYPE_BINDINGS_COMPILER.toString());
            compiler.add("config", new JsonObject());
            root.add("compiler", compiler);
        }

        return root;
    }

    private static void invokeSetBlockEntity(InMemoryPartitionAccess world, BlockPos pos, ResourceLocation typeId) throws Exception {
        Method method = InMemoryPartitionAccess.class.getMethod("setBlockEntity", BlockPos.class, ResourceLocation.class);
        method.invoke(world, pos, typeId);
    }
}

