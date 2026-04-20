package dev.vfyjxf.cumulus.gametest;

import dev.vfyjxf.cumulus.Cumulus;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.target.Targets;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.gametest.DynamicStructureTemplates;
import net.neoforged.testframework.gametest.StructureTemplateBuilder;

@EventBusSubscriber(modid = Cumulus.MODID)
@GameTestHolder(Cumulus.MODID)
public final class CumulusGameTests {
    private static final ResourceLocation TEMPLATE_ID = ResourceLocation.fromNamespaceAndPath(
            Cumulus.MODID,
            "definitiondslbuildsinsidegametest");
    private static final DynamicStructureTemplates TEMPLATES = new DynamicStructureTemplates();

    private CumulusGameTests() {
    }

    @SubscribeEvent
    private static void registerDynamicTemplates(ServerAboutToStartEvent event) {
        if (!GameTestHooks.isGametestServer()) {
            return;
        }
        TEMPLATES.register(TEMPLATE_ID, StructureTemplateBuilder.empty(3, 3, 3));
        try {
            TEMPLATES.setup(event.getServer().getStructureManager());
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to register Cumulus GameTest templates", throwable);
        }
    }

    @GameTest
    @PrefixGameTestTemplate(false)
    public static void definitionDslBuildsInsideGameTest(GameTestHelper helper) {
        var definition = Structures.define(ResourceLocation.parse("cumulus:gametest_smoke"))
                .point("origin", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .build();

        if (definition.points().size() != 1) {
            throw new GameTestAssertException("Expected one structure point");
        }

        helper.succeed();
    }
}
