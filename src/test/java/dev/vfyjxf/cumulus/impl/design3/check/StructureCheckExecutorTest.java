package dev.vfyjxf.cumulus.impl.design3.check;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.predicate.PredicateRegistry;
import dev.vfyjxf.cumulus.api.runtime.InstanceStatus;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import dev.vfyjxf.cumulus.impl.design3.StructureRuntimePlanCompiler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureCheckExecutorTest {
    @Test
    void validatesBlockEntityEntityFluidContextAndRegionTargets() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:complex_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller"))
                        .capture("controller"))
                .point("inventory", RelativeBlockPos.of(1, 0, 0), Targets.blockEntity()
                        .where(PredicateDescriptors.id("cumulus:item_handler"))
                        .capture("inventory"))
                .point("worker", RelativeBlockPos.of(0, 1, 0), Targets.entity()
                        .where(PredicateDescriptors.id("cumulus:worker")))
                .point("coolant", RelativeBlockPos.of(0, 0, 1), Targets.fluid()
                        .where(PredicateDescriptors.id("minecraft:water")))
                .point("energy", RelativeBlockPos.of(1, 1, 0), Targets.capability("energy")
                        .where(PredicateDescriptors.property("side", "north")))
                .point("facing", RelativeBlockPos.of(0, 0, 0), Targets.context("facing")
                        .where(PredicateDescriptors.id("north")))
                .component(Components.region("door", RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 2, 0))
                        .where(PredicateDescriptors.id("cumulus:door_frame")))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .blockEntity(RelativeBlockPos.of(1, 0, 0), "cumulus:item_handler")
                .entity(RelativeBlockPos.of(0, 1, 0), "cumulus:worker")
                .fluid(RelativeBlockPos.of(0, 0, 1), "minecraft:water")
                .capability(RelativeBlockPos.of(1, 1, 0), "energy", Map.of("side", "north"))
                .context("facing", "north")
                .region(RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 2, 0), "cumulus:door_frame")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(10, 64, 10), snapshot);

        assertEquals(InstanceStatus.VALID, result.status());
        assertEquals("10,64,10", result.context().values().get("controller"));
        assertEquals("11,64,10", result.context().values().get("inventory"));
        assertEquals(0, result.diagnostics().nodes().size());
    }

    @Test
    void reportsDiagnosticGraphForFailedRegionPredicate() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:broken_door"))
                .component(Components.region("door", RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 2, 0))
                        .where(PredicateDescriptors.id("cumulus:door_frame")))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .region(RelativeBlockPos.of(-1, 0, 0), RelativeBlockPos.of(1, 2, 0), "minecraft:air")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 64, 0), snapshot);

        assertEquals(InstanceStatus.INVALID, result.status());
        assertEquals("root", result.diagnostics().rootNodeId());
        assertEquals(2, result.diagnostics().nodes().size());
        assertEquals("door", result.diagnostics().childrenOf("root").getFirst().id());
        assertEquals(Severity.ERROR, result.diagnostics().childrenOf("root").getFirst().severity());
        assertTrue(result.visualization().elements().stream().anyMatch(element -> element.id().equals("door-error")));
    }

    @Test
    void missingTargetProducesIncompleteDataRequest() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:missing"))
                .point("controller", RelativeBlockPos.of(32, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 64, 0), InMemoryTargetSnapshot.empty());

        assertEquals(InstanceStatus.INCOMPLETE, result.status());
        assertEquals(1, result.requests().size());
        assertEquals("controller", result.diagnostics().childrenOf("root").getFirst().id());
    }

    @Test
    void genericPropertyPredicateMatchesSnapshotDescriptors() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:property_machine"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.property("tier", "advanced")))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller", Map.of("tier", "advanced"))
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);

        assertEquals(InstanceStatus.VALID, result.status());
    }

    @Test
    void registeredPredicateEvaluatesSerializedDescriptorProperties() {
        var registry = PredicateRegistry.defaults()
                .register(ResourceLocation.parse("cumulus:has_tag"), (predicate, target) -> {
                    var requestedTag = predicate.properties().get("tag");
                    var tags = target.properties().getOrDefault("tags", "");
                    return requestedTag != null && List.of(tags.split(",")).contains(requestedTag);
                });
        var serialized = new LinkedHashMap<String, String>();
        serialized.put("id", "cumulus:has_tag");
        serialized.put("tag", "heated");
        var definition = Structures.define(ResourceLocation.parse("cumulus:registered_predicate"))
                .point("heater", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.fromSerialized(serialized)))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:heater", Map.of("tags", "heated,active"))
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot, registry);

        assertEquals(InstanceStatus.VALID, result.status());
    }

    @Test
    void registeredPropertylessPredicateUsesCustomMatcherBeforeIdFallback() {
        var registry = PredicateRegistry.defaults()
                .register(ResourceLocation.parse("cumulus:is_hot"), (predicate, target) ->
                        "true".equals(target.properties().get("hot")));
        var definition = Structures.define(ResourceLocation.parse("cumulus:propertyless_custom_predicate"))
                .point("heater", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(new PredicateDescriptor(ResourceLocation.parse("cumulus:is_hot"), Map.of())))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:heater", Map.of("hot", "true"))
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot, registry);

        assertEquals(InstanceStatus.VALID, result.status());
    }

    @Test
    void serializedPredicateLoadingSortsPropertiesForStableAssets() {
        var serialized = new LinkedHashMap<String, String>();
        serialized.put("id", "cumulus:range");
        serialized.put("max", "9");
        serialized.put("min", "3");

        var descriptor = PredicateDescriptors.fromSerialized(serialized);

        assertEquals(ResourceLocation.parse("cumulus:range"), descriptor.id());
        assertEquals(List.of("max", "min"), new ArrayList<>(descriptor.properties().keySet()));
    }

    @Test
    void optionalMissingTargetDoesNotInvalidateStructure() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:optional_port"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .point("optional_port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port")))
                .build();
        var optionalDefinition = new dev.vfyjxf.cumulus.api.definition.StructureDefinition(
                definition.id(),
                definition.schemaVersion(),
                definition.transforms(),
                java.util.List.of(
                        definition.points().get(0),
                        new dev.vfyjxf.cumulus.api.definition.StructurePointDefinition(
                                definition.points().get(1).id(),
                                definition.points().get(1).location(),
                                definition.points().get(1).target(),
                                definition.points().get(1).predicates(),
                                dev.vfyjxf.cumulus.api.definition.PointRequirement.OPTIONAL,
                                definition.points().get(1).captures(),
                                definition.points().get(1).groups(),
                                definition.points().get(1).diagnostics())),
                definition.components(),
                definition.contextRules(),
                definition.versions(),
                definition.migrations(),
                definition.diagnostics());
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .build();

        var result = StructureCheckExecutor.check(optionalDefinition, new BlockPos(0, 0, 0), snapshot);

        assertEquals(InstanceStatus.VALID, result.status());
        assertEquals(0, result.requests().size());
    }

    @Test
    void unsupportedPredicateFailsWithDiagnosticInsteadOfPassingSilently() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:unsupported"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(new PredicateDescriptor(ResourceLocation.parse("cumulus:unsupported_predicate"), Map.of("mode", "strict"))))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);

        assertEquals(InstanceStatus.INVALID, result.status());
        assertEquals("controller", result.diagnostics().childrenOf("root").getFirst().id());
    }

    @Test
    void unsupportedTargetTypeIsInvalidWithoutRequestingMoreData() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:unknown_target"))
                .point("controller",
                        RelativeBlockPos.of(0, 0, 0),
                        new dev.vfyjxf.cumulus.api.target.TargetPattern(
                                new dev.vfyjxf.cumulus.api.target.TargetDescriptor(
                                        ResourceLocation.parse("cumulus:unknown_target"),
                                        Map.of("name", "energy")),
                                java.util.List.of(PredicateDescriptors.id("cumulus:unknown")),
                                java.util.Set.of()))
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), InMemoryTargetSnapshot.empty());

        assertEquals(InstanceStatus.INVALID, result.status());
        assertEquals(0, result.requests().size());
        var evidence = result.diagnostics().childrenOf("root").getFirst().evidence().getFirst();
        assertEquals("UNSUPPORTED_TARGET", evidence.detail());
        assertEquals("cumulus:unknown_target", evidence.targetId());
    }

    @Test
    void unsupportedPredicateDiagnosticNamesTheUnsupportedPredicate() {
        var unsupportedPredicate = new PredicateDescriptor(
                ResourceLocation.parse("cumulus:unsupported_predicate"),
                Map.of("mode", "strict"));
        var definition = Structures.define(ResourceLocation.parse("cumulus:unsupported_predicate"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(unsupportedPredicate))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);

        var evidence = result.diagnostics().childrenOf("root").getFirst().evidence().getFirst();
        assertEquals("UNSUPPORTED_PREDICATE", evidence.detail());
        assertEquals("cumulus:unsupported_predicate", evidence.predicateId());
    }

    @Test
    void predicateChainEvidenceNamesFirstFailedPredicate() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:predicate_chain"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller"))
                        .where(PredicateDescriptors.property("tier", "advanced"))
                        .where(PredicateDescriptors.property("enabled", "true")))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller", Map.of(
                        "tier", "basic",
                        "enabled", "false"))
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);

        var evidence = result.diagnostics().childrenOf("root").getFirst().evidence().getFirst();
        assertEquals("PREDICATE_FAILED", evidence.detail());
        assertEquals("cumulus:property", evidence.predicateId());
        assertEquals("tier", evidence.descriptors().get("predicate.key"));
    }

    @Test
    void contextMissingAndContextMismatchHaveDistinctDiagnostics() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:facing_machine"))
                .point("facing", RelativeBlockPos.of(0, 0, 0), Targets.context("facing")
                        .where(PredicateDescriptors.id("north")))
                .build();
        var missing = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), InMemoryTargetSnapshot.empty());
        var mismatched = StructureCheckExecutor.check(
                definition,
                new BlockPos(0, 0, 0),
                InMemoryTargetSnapshot.builder().context("facing", "south").build());

        assertEquals(InstanceStatus.INCOMPLETE, missing.status());
        assertEquals("MISSING_CONTEXT", missing.diagnostics().childrenOf("root").getFirst().evidence().getFirst().detail());
        assertEquals(InstanceStatus.INVALID, mismatched.status());
        assertEquals("PREDICATE_FAILED", mismatched.diagnostics().childrenOf("root").getFirst().evidence().getFirst().detail());
        assertEquals(0, mismatched.requests().size());
    }

    @Test
    void blockAndBlockEntityTargetsAtSameRelativePositionDoNotOverwriteEachOther() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:same_position"))
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .point("inventory", RelativeBlockPos.of(0, 0, 0), Targets.blockEntity()
                        .where(PredicateDescriptors.id("cumulus:item_handler")))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .blockEntity(RelativeBlockPos.of(0, 0, 0), "cumulus:item_handler")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);

        assertEquals(InstanceStatus.VALID, result.status());
    }

    @Test
    void declaredRotationCandidateCanValidateStructure() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:rotated_machine"))
                .transforms(Transforms.identity(), Transforms.rotateY90())
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port"))
                        .capture("port"))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .block(RelativeBlockPos.of(0, 0, 1), "cumulus:port")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(10, 64, 10), snapshot);

        assertEquals(InstanceStatus.VALID, result.status());
        assertEquals("10,64,11", result.context().values().get("port"));
    }

    @Test
    void fastCheckPlanEvaluationShortCircuitsAfterFirstValidCandidate() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:fast_rotated_machine"))
                .transforms(Transforms.identity(), Transforms.rotateY90(), Transforms.rotateY180())
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port")))
                .build();
        var plan = StructureRuntimePlanCompiler.compileCheckPlan(definition);
        var snapshot = InMemoryTargetSnapshot.builder()
                .block(RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .block(RelativeBlockPos.of(0, 0, 1), "cumulus:port")
                .build();

        var result = StructureCheckExecutor.fastCheck(plan, snapshot);

        assertEquals(InstanceStatus.VALID, result.status());
        assertEquals(2, result.candidatesEvaluated());
        assertEquals(4, result.entriesEvaluated());
        assertEquals(1, result.validCandidateIndex());
    }

    @Test
    void fastCheckReadsCandidateTargetsFromSourceAndStopsAfterValidCandidate() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:fast_source_rotated_machine"))
                .transforms(Transforms.identity(), Transforms.rotateY90(), Transforms.rotateY180())
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller")))
                .point("port", RelativeBlockPos.of(1, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:port")))
                .build();
        var plan = StructureRuntimePlanCompiler.compileCheckPlan(definition);
        var source = new CountingSource()
                .put(ResourceLocation.parse("cumulus:block"), RelativeBlockPos.of(0, 0, 0), "cumulus:controller")
                .put(ResourceLocation.parse("cumulus:block"), RelativeBlockPos.of(0, 0, 1), "cumulus:port");

        var result = StructureCheckExecutor.fastCheck(plan, source);

        assertEquals(InstanceStatus.VALID, result.status());
        assertEquals(2, result.candidatesEvaluated());
        assertEquals(4, result.entriesEvaluated());
        assertEquals(4, source.reads);
        assertEquals(1, result.validCandidateIndex());
    }

    @Test
    void declaredMirrorCandidateCanValidateRegionBounds() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:mirrored_region"))
                .transforms(Transforms.identity(), Transforms.horizontalMirror())
                .component(Components.region("left_wing", RelativeBlockPos.of(1, 0, 0), RelativeBlockPos.of(2, 0, 0))
                        .where(PredicateDescriptors.id("cumulus:wing")))
                .build();
        var snapshot = InMemoryTargetSnapshot.builder()
                .region(RelativeBlockPos.of(-2, 0, 0), RelativeBlockPos.of(-1, 0, 0), "cumulus:wing")
                .build();

        var result = StructureCheckExecutor.check(definition, new BlockPos(0, 0, 0), snapshot);

        assertEquals(InstanceStatus.VALID, result.status());
    }

    private static final class CountingSource implements TargetSnapshotSource {
        private final Map<Key, SnapshotTarget> targets = new LinkedHashMap<>();
        private int reads;

        CountingSource put(ResourceLocation targetType, RelativeBlockPos pos, String value) {
            targets.put(new Key(targetType, pos, pos), new SnapshotTarget(value, Map.of()));
            return this;
        }

        @Override
        public java.util.Optional<SnapshotTarget> target(
                ResourceLocation targetType,
                RelativeBlockPos min,
                RelativeBlockPos max) {
            reads++;
            return java.util.Optional.ofNullable(targets.get(new Key(targetType, min, max)));
        }

        @Override
        public java.util.Optional<String> context(String key) {
            return java.util.Optional.empty();
        }

        private record Key(ResourceLocation targetType, RelativeBlockPos min, RelativeBlockPos max) {
        }
    }
}
