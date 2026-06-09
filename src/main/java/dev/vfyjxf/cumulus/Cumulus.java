package dev.vfyjxf.cumulus;

import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.impl.codec.RuntimeAssetReloadListener;
import dev.vfyjxf.cumulus.impl.codec.RuntimeAssetRepository;
import dev.vfyjxf.cumulus.impl.editor.StructureEditorController;
import dev.vfyjxf.cumulus.impl.minecraft.MinecraftRuntimeEventBridge;
import dev.vfyjxf.cumulus.impl.minecraft.MinecraftRuntimeInstanceTracker;
import dev.vfyjxf.cumulus.impl.runtime.RuntimeAssetEngineBridge;
import dev.vfyjxf.cumulus.impl.runtime.RuntimeAssetReloadWiring;
import dev.vfyjxf.cumulus.impl.runtime.RuntimeCheckEngine;
import dev.vfyjxf.cumulus.impl.runtime.RuntimeCheckEngineTickDriver;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Cumulus.MODID)
public final class Cumulus {
    public static final String MODID = "cumulus";

    private final RuntimeCheckEngine runtimeCheckEngine = new RuntimeCheckEngine();
    private final RuntimeAssetRepository runtimeAssetRepository = new RuntimeAssetRepository();
    private final RuntimeAssetEngineBridge runtimeAssetEngineBridge =
            new RuntimeAssetEngineBridge(runtimeAssetRepository, runtimeCheckEngine, Structures.registry());
    private final RuntimeCheckEngineTickDriver runtimeTickDriver = new RuntimeCheckEngineTickDriver(runtimeCheckEngine);
    private final MinecraftRuntimeInstanceTracker minecraftRuntimeInstanceTracker =
            new MinecraftRuntimeInstanceTracker(runtimeCheckEngine);
    private final MinecraftRuntimeEventBridge minecraftRuntimeEventBridge =
            new MinecraftRuntimeEventBridge(minecraftRuntimeInstanceTracker);
    private final RuntimeAssetReloadListener runtimeAssetReloadListener =
            new RuntimeAssetReloadListener(runtimeAssetRepository, this::registerReloadedDefinitions);
    private final StructureEditorController structureEditorController = new StructureEditorController();

    public Cumulus(IEventBus modEventBus, ModContainer modContainer) {
        new RuntimeAssetReloadWiring(runtimeAssetReloadListener).register(NeoForge.EVENT_BUS);
        runtimeTickDriver.register(NeoForge.EVENT_BUS);
        minecraftRuntimeEventBridge.register(NeoForge.EVENT_BUS);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientOnly.registerStructureEditor(modEventBus, NeoForge.EVENT_BUS, structureEditorController);
        }
    }

    public StructureEditorController structureEditorController() {
        return structureEditorController;
    }

    private java.util.List<net.minecraft.resources.ResourceLocation> registerReloadedDefinitions() {
        var registered = runtimeAssetEngineBridge.registerDefinitions();
        for (var asset : runtimeAssetRepository.assets()) {
            asset.definition().ifPresent(minecraftRuntimeEventBridge.discovery()::register);
        }
        return registered;
    }

    private static final class ClientOnly {
        private ClientOnly() {
        }

        private static void registerStructureEditor(
                IEventBus modEventBus,
                IEventBus gameEventBus,
                StructureEditorController controller) {
            try {
                var type = Class.forName("dev.vfyjxf.cumulus.impl.editor.StructureEditorClientBridge");
                var bridge = type.getConstructor(StructureEditorController.class).newInstance(controller);
                type.getMethod("register", IEventBus.class, IEventBus.class)
                        .invoke(bridge, modEventBus, gameEventBus);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to register structure editor client bridge", exception);
            }
        }
    }
}
