# ArchitectCumulus

ArchitectCumulus is a NeoForge library for defining and validating flexible multiblock-style structures.

Start here for the current `design3` work:

- Design overview: `DESIGN3.md`
- Usage examples: `USAGE.md`
- Implementation map: `IMPLEMENTATION.md`
- Current code file list and Git note: `DESIGN3_CODE_FILES.md`
- Full design spec: `docs/superpowers/specs/2026-06-08-architect-cumulus-design3.md`

The current `design3` implementation combines the definition-focused `design2` branch with the runtime concepts from `design`:

- immutable in-memory structure IR;
- Java definition DSL;
- point, filled box, hollow box, region, and substructure components;
- block, block entity, entity, fluid, capability, region, and context target descriptors;
- transform, context derivation, version, and migration descriptors;
- diagnostic graph, fix-hint, visualization, and datagen asset contracts;
- section-level data requests, runtime context deltas, and shared/exclusive claim contracts;
- a small runtime plan compiler and instance manager that derive footprint sections, prefetch requests, build hints, visualization elements, and revisioned runtime context deltas from structure IR;
- a runtime asset exporter for stable datagen/runtime descriptors;
- immutable transform-aware check plans with precomputed point and region bounds for repeated runtime checks;
- in-memory diagnostic and fast check executors for block, block entity, entity, fluid, capability, context, and region targets, including short-circuit compiled-plan evaluation when a valid transform candidate is found;
- a target snapshot source/reader adapter for feeding checks from world-like data providers;
- a Minecraft `LevelAccessor` snapshot source for loaded block, block entity, entity, fluid, NeoForge block capability resolver, region, and context reads;
- predicate registry and serialized predicate descriptor loading for custom check logic;
- a budgeted runtime scheduler with dirty-section indexing, deduplicated pending checks, and request-ledger integration;
- a runtime check engine that wires dirty-section ticks to snapshot reading, structure checks, deltas, and data requests, plus a status-only fast tick path for compiled-plan validation telemetry;
- runtime substructure composition that checks registered child points and regions at child anchor offsets, publishes independent child occurrence deltas/diagnostics with parent ownership/state metadata, propagates child status or opt-in child context snapshots back into the parent context, emits lifecycle callbacks for child occurrence additions/status changes, detects child occurrence ownership conflicts, and includes missing/cyclic child preflight diagnostics;
- a NeoForge server post-tick driver registered from the mod entry point for bounded runtime check draining, using status-only fast ticks by default with an explicit diagnostic-tick request path;
- deterministic runtime asset datagen export-set planning;
- JSON/map-list runtime asset loaders with deterministic ordering and diagnostic reporting for invalid datapack-style content;
- a runtime asset repository that atomically indexes valid reload snapshots by asset id and definition id;
- a NeoForge server datapack reload listener registered from the mod entry point for `data/*/cumulus/structures/*.json` runtime assets;
- embedded-definition runtime asset round-tripping and a reload-to-engine bridge that registers embedded or registry-resolved id-only definitions into the tick-driven runtime engine after valid reloads;
- a Minecraft runtime instance tracking facade, discovery registry with explicit-anchor and inferred required-point candidates, and event bridge that register `LevelAccessor` snapshot sources, untrack anchors, and mark changed block sections dirty from block mutation events;
- an in-memory automatic build action executor for point, region, capability backing block, and capability mutation build plans;
- a Minecraft `LevelAccessor` build target for idempotent block, region, fluid, capability backing block, optional typed energy/item/fluid capability mutation writer policies with built-in idempotent ensure operations, block entity NBT placement, and entity spawning with authored NBT, with explicit unsupported handling when no capability mutation writer is supplied;
- a client-wired ghost preview/editor seed with runtime-plan preview boxes, active editor sessions, selection cycling, render command styling, key mappings, and NeoForge level-render event line-box rendering;
- in-memory substructure graph resolution, tick-driven substructure check composition, and runtime context migration execution slices;
- plain JUnit, NeoForge JUnit, and GameTest coverage.

```java
var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
    .transforms(Transforms.identity(), Transforms.horizontalFacing(), Transforms.rotateY90())
    .point("controller", RelativeBlockPos.of(0, 1, 0), Targets.block()
        .where(PredicateDescriptors.id("cumulus:controller"))
        .capture("controller"))
    .component(Components.region("door",
        RelativeBlockPos.of(-1, 0, 0),
        RelativeBlockPos.of(1, 2, 0))
        .target(Targets.region(
            RelativeBlockPos.of(-1, 0, 0),
            RelativeBlockPos.of(1, 2, 0))))
    .component(Components.substructure("left_leaf",
        ResourceLocation.parse("cumulus:door_leaf"),
        RelativeBlockPos.of(-1, 0, 0)))
    .component(Components.hollowBox("casing",
        RelativeBlockPos.of(-1, 0, -1),
        RelativeBlockPos.of(1, 2, 1))
        .target(Targets.block())
        .where(PredicateDescriptors.id("cumulus:casing")))
    .context(ContextRuleDescriptor.derive("facing", "controller.facing"))
    .version(new StructureVersionDescriptor(2, ResourceLocation.parse("cumulus:machine/v2")))
    .build();
```

## Verification

```bash
./gradlew test
./gradlew runGameTestServer
```
