package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.context.ContextRuleDescriptor;
import dev.vfyjxf.cumulus.api.context.ContextSources;
import dev.vfyjxf.cumulus.api.context.plan.CountConstraint;
import dev.vfyjxf.cumulus.api.context.plan.PhaseResult;
import dev.vfyjxf.cumulus.api.migration.StructureVersionDescriptor;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.predicate.StructurePredicates;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import dev.vfyjxf.cumulus.api.types.ContextKey;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.vfyjxf.cumulus.api.definition.Structures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Structures#define(String, java.util.function.Function)} —
 * the lambda-wrapped definition that allows an entire structure (including
 * local PartPattern declarations) to be expressed in a single {@code static final} field.
 */
class StructuresDefineTest {

    @Test
    void lambdaDefineProducesSameResultAsChained() {
        // Lambda form
        var lambdaDef = define("cumulus:lambda_test", def -> def.point("controller", RelativeBlockPos.at(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).build());

        // Chained form
        var chainedDef = define(id("cumulus:lambda_test")).point("controller", RelativeBlockPos.at(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).build();

        assertEquals(chainedDef.id(), lambdaDef.id());
        assertEquals(chainedDef.points().size(), lambdaDef.points().size());
        assertEquals(chainedDef.schemaVersion(), lambdaDef.schemaVersion());
    }

    @Test
    void lambdaCanDeclareLocalPartPatternsAndShape() {
        var def = define("cumulus:shape_test", def_ -> {
            var casing = PartPattern.block("casing").where(PredicateDescriptors.id("cumulus:heat_casing")).group("casing");
            var ctrl = PartPattern.block("controller").where(PredicateDescriptors.id("cumulus:controller")).capture("controller");

            return def_.shape(new PartPattern[][][]{{{casing, casing, casing}, {casing, ctrl, casing}, {casing, casing, casing},
            }}).transforms(Transforms.horizontalFacing()).build();
        });

        assertNotNull(def);
        assertEquals(ResourceLocation.parse("cumulus:shape_test"), def.id());
        // 3×3 = 9 points (all non-air)
        assertEquals(9, def.points().size());
    }

    @Test
    void lambdaWithCheckPhaseAndConstraints() {
        var def = define("cumulus:phase_test", def_ -> def_.point("controller", RelativeBlockPos.at(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).checkPhase(ctx -> PhaseResult.of(List.of(), List.of(CountConstraint.atLeast(StructurePredicates.group("casing"), 1)))).build());

        assertNotNull(def);
        assertNotNull(def.checkPhase());
    }

    @Test
    void lambdaWithContextRules() {
        var def = define("cumulus:ctx_test", def_ -> def_.point("controller", RelativeBlockPos.at(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller")).capture("controller")).context(ContextRuleDescriptor.derive(ContextKey.string("facing"), ContextSources.capture("controller").property("facing"))).build());

        assertEquals(1, def.contextRules().size());
        assertEquals("facing", def.contextRules().get(0).targetKey().name());
    }

    @Test
    void lambdaWithComponents() {
        var def = define("cumulus:comp_test", def_ -> def_.point("controller", RelativeBlockPos.at(0, 3, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).component(Components.hollowBox("casing", RelativeBlockPos.at(-1, 0, -1), RelativeBlockPos.at(1, 2, 1)).where(PredicateDescriptors.id("cumulus:heat_casing"))).build());

        assertNotNull(def);
        assertTrue(def.points().size() > 1); // controller + hollowBox shell
    }

    @Test
    void lambdaWithVersionAndMigration() {
        var def = define("cumulus:ver_test", def_ -> def_.point("controller", RelativeBlockPos.at(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).version(new StructureVersionDescriptor(
                        2, ResourceLocation.parse("cumulus:machine/v2"))).build());

        assertEquals(1, def.versions().size());
        assertEquals(2, def.versions().get(0).version());
    }

    @Test
    void lambdaRejectsNullId() {
        assertThrows(IllegalArgumentException.class, () -> define(null, def -> def.build()));
    }

    @Test
    void lambdaRejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> define("  ", def -> def.build()));
    }

    @Test
    void lambdaRejectsNullRecipe() {
        assertThrows(IllegalArgumentException.class, () -> define("cumulus:test", null));
    }

    @Test
    void lambdaRecipeCanReturnPreBuilt() {
        // The recipe can ignore the builder and return a pre-built definition
        var preBuilt = define(id("cumulus:pre_built")).point("controller", RelativeBlockPos.at(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).build();

        var result = define("cumulus:wrapper", def -> preBuilt);

        assertEquals(preBuilt, result);
    }

    @Test
    void lambdaWithEmptyShape() {
        var def = define("cumulus:empty_shape", def_ -> def_.shape(new PartPattern[][][]{{}}).point("controller", RelativeBlockPos.at(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).build());

        // Empty shape (0 columns in the only layer) → no shape points, only the explicit point
        assertEquals(1, def.points().size());
    }

    @Test
    void lambdaWithAirCellsSkipped() {
        var air = PartPattern.air();
        var ctrl = PartPattern.block("controller").where(PredicateDescriptors.id("cumulus:controller"));

        var def = define("cumulus:air_test", def_ -> def_.shape(new PartPattern[][][]{{{air, ctrl, air},
        }}).build());

        // 3 cells but 2 are air → only 1 point
        assertEquals(1, def.points().size());
    }

    @Test
    void idShortcutParsesCorrectly() {
        var rl = id("cumulus:my_machine");
        assertEquals(ResourceLocation.parse("cumulus:my_machine"), rl);
        assertEquals("cumulus", rl.getNamespace());
        assertEquals("my_machine", rl.getPath());
    }

    @Test
    void defineStringShortcutEqualsDefineResourceLocation() {
        var fromString = define("cumulus:shortcut");
        var fromRL = define(id("cumulus:shortcut"));
        assertEquals(fromRL.id(), fromString.id());
    }

    @Test
    void lambdaProducesValidImmutableDefinition() {
        var def = define("cumulus:immutable_test", def_ -> def_.point("controller", RelativeBlockPos.at(0, 0, 0), Targets.block().where(PredicateDescriptors.id("cumulus:controller"))).transforms(Transforms.identity()).build());

        // Verify immutability — lists should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> def.points().add(null));
        assertThrows(UnsupportedOperationException.class, () -> def.transforms().add(null));
    }
}
