package dev.vfyjxf.cumulus.api.context.sources;

import dev.vfyjxf.cumulus.api.context.CheckContext;
import dev.vfyjxf.cumulus.api.context.ContextSources;
import dev.vfyjxf.cumulus.api.definition.StructurePointId;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaptureSourceTest {

    private static final StructurePointId CTRL = new StructurePointId("controller");

    @Test
    void valueFieldReadsPrimaryValue() {
        var source = ContextSources.capture("controller").value();
        var ctx = new CheckContext();
        ctx.putResolvedTarget(CTRL, new CheckContext.ResolvedTarget("cumulus:controller", Map.of(), "10,64,10"));
        assertEquals(Optional.of("cumulus:controller"), source.resolve(ctx));
    }

    @Test
    void positionFieldReadsAbsolutePosition() {
        var source = ContextSources.capture("controller").position();
        var ctx = new CheckContext();
        ctx.putResolvedTarget(CTRL, new CheckContext.ResolvedTarget("cumulus:controller", Map.of(), "10,64,10"));
        assertEquals(Optional.of("10,64,10"), source.resolve(ctx));
    }

    @Test
    void propertyFieldReadsNamedProperty() {
        var source = ContextSources.capture("controller").property("facing");
        var ctx = new CheckContext();
        ctx.putResolvedTarget(CTRL, new CheckContext.ResolvedTarget("cumulus:controller", Map.of("facing", "north"), "0,0,0"));
        assertEquals(Optional.of("north"), source.resolve(ctx));
    }

    @Test
    void missingPointReturnsEmpty() {
        var source = ContextSources.capture("absent").value();
        var ctx = new CheckContext();
        assertEquals(Optional.empty(), source.resolve(ctx));
    }

    @Test
    void captureFieldPropertyRejectsNull() {
        assertThrows(NullPointerException.class, () -> CaptureField.property(null));
    }

    @Test
    void prefixShiftsCaptureSourcePoint() {
        var original = ContextSources.capture("controller").property("facing");
        var prefixed = ContextSources.prefix(original, "left_arm");
        // Prefixed source should have point "left_arm/controller"
        var ctx = new CheckContext();
        ctx.putResolvedTarget(new StructurePointId("left_arm/controller"), new CheckContext.ResolvedTarget("cumulus:controller", Map.of("facing", "east"), "5,5,5"));
        assertEquals(Optional.of("east"), prefixed.resolve(ctx));
    }
}
