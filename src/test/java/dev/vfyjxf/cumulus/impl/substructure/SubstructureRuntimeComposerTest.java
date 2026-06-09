package dev.vfyjxf.cumulus.impl.substructure;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.Targets;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubstructureRuntimeComposerTest {
    @Test
    void composesNestedChildPointsAtCumulativeOffsets() {
        var parentId = ResourceLocation.parse("cumulus:composed_parent");
        var childId = ResourceLocation.parse("cumulus:composed_child");
        var leafId = ResourceLocation.parse("cumulus:composed_leaf");
        var parent = Structures.define(parentId)
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .component(Components.substructure("child", childId, RelativeBlockPos.of(2, 0, 0)))
                .build();
        var child = Structures.define(childId)
                .component(Components.substructure("leaf", leafId, RelativeBlockPos.of(0, 3, 0)))
                .build();
        var leaf = Structures.define(leafId)
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port"))
                        .capture("port"))
                .build();
        var definitions = Map.of(
                parent.id(), parent,
                child.id(), child,
                leaf.id(), leaf);
        var graph = SubstructureGraphResolver.resolve(parentId, definitions).graph().orElseThrow();

        var composed = SubstructureRuntimeComposer.compose(parentId, definitions, graph);

        var leafPoint = composed.points().stream()
                .filter(point -> point.id().value().endsWith("/port"))
                .findFirst()
                .orElseThrow();
        assertEquals("cumulus:composed_parent/child/leaf/port", leafPoint.id().value());
        assertEquals(RelativeBlockPos.of(3, 3, 0), leafPoint.location());
        assertEquals(
                "cumulus:composed_parent/child/leaf/port",
                leafPoint.captures().iterator().next());
    }

    @Test
    void translatesChildRegionTargetBounds() {
        var parentId = ResourceLocation.parse("cumulus:region_parent");
        var childId = ResourceLocation.parse("cumulus:region_child");
        var parent = Structures.define(parentId)
                .component(Components.substructure("door", childId, RelativeBlockPos.of(4, 0, 1)))
                .build();
        var child = Structures.define(childId)
                .component(Components.region(
                        "frame",
                        RelativeBlockPos.of(-1, 0, 0),
                        RelativeBlockPos.of(1, 2, 0))
                        .where(PredicateDescriptors.id("cumulus:frame")))
                .build();
        var definitions = Map.of(parent.id(), parent, child.id(), child);
        var graph = SubstructureGraphResolver.resolve(parentId, definitions).graph().orElseThrow();

        var composed = SubstructureRuntimeComposer.compose(parentId, definitions, graph);

        var region = composed.points().stream()
                .filter(point -> point.id().value().endsWith("/frame"))
                .findFirst()
                .orElseThrow();
        assertEquals(RelativeBlockPos.of(3, 0, 1), region.location());
        assertEquals("3,0,1", region.target().properties().get("from"));
        assertEquals("5,2,1", region.target().properties().get("to"));
    }
}
