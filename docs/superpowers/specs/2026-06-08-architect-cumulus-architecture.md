# ArchitectCumulus Architecture

Date: 2026-06-08

## Purpose

The current architecture combines the two previous architecture directions into one staged multiblock foundation.

- `design2` supplies the author-facing Java DSL and immutable structure IR: definitions, points, components, targets, predicates, transforms, context descriptors, validation, and tests.
- `design` supplies the runtime engine concepts: section partitioning, incomplete data requests, budgeted ticks, instance keys, runtime contexts, diagnostics, fix hints, interest sets, and cross-instance dependency thinking.
- GitHub issue 36 supplies the feature envelope: visualization, automatic construction, high performance, data-driven structures, datagen, multi-level error analysis, non-block predicates, arbitrary rectangular predicates, substructures, runtime context conversion, in-game design, upgrade migration, rotations, and shared/exclusive structure parts.

The current architecture keeps Java definitions as the source of truth, compiles them into stable IR, and makes runtime/debug/datagen consumers read that IR instead of fluent builder state.

## Layered Model

```text
Java DSL definitions
  -> immutable StructureDefinition IR
  -> runtime/debug/datagen assets
  -> compiled check and diagnostic plans
  -> StructureInstanceManager / engine
  -> RuntimeStructureContext, DiagnosticGraph, VisualizationModel
```

The current implementation lands the API contracts for the first three layers and the runtime-facing value contracts needed by later managers.
It also includes a small runtime plan compiler and instance manager slice so the IR can already produce footprint sections, prefetch requests, build hints, visualization elements, and versioned runtime context deltas.
`StructureCheckPlan` adds an immutable transform-aware check plan with precomputed target bounds per transform candidate, and both snapshot reading and check execution can consume it directly.
`RuntimeAssetExporter` adds a stable descriptor export path for runtime/datagen assets.
`StructureCheckExecutor` adds the first executable checking slice over an in-memory target snapshot, covering block, block entity, entity, fluid, capability, context, and region targets.
It evaluates declared simple rotation and mirror transform candidates against the full definition and uses the first valid candidate.
It also exposes a `fastCheck` compiled-plan path for production-style loops that only need status and evaluation counters, short-circuiting after the first valid transform candidate while leaving diagnostic graph generation to the full check path.
`TargetSnapshotReader` adds a world-agnostic source adapter so real Level/data-provider integrations can feed the same check executor without changing the check contract.
`MinecraftTargetSnapshotSource` adds the first production-facing `LevelAccessor` adapter for loaded block, block entity, entity, fluid, NeoForge block capability resolver, region, and context reads while preserving the request-not-load boundary for missing chunks.
`PredicateRegistry` lets check execution use built-in or custom predicate matchers loaded from serialized descriptor maps.
`SubstructureGraphResolver` and `StructureContextMigrationExecutor` add first executable slices for nested substructure composition and versioned runtime context migration.
`RuntimeAssetDatagenProvider` and `BuildPlanExecutor` add deterministic export-set planning and in-memory automatic build action execution, including capability mutation intent metadata.
`RuntimeScheduler` adds a budgeted dirty-section runtime loop with deterministic instance ordering, deduplicated pending checks, queued-work carryover across frames, and `RequestLedger` integration.
`RuntimeCheckEngine` wires that scheduler to registered definitions, tracked anchors, `TargetSnapshotSource` adapters, `TargetSnapshotReader`, and `StructureCheckExecutor` so dirty sections now produce checked `InstanceDelta`s and data requests through a production-shaped lifecycle component, and exposes a status-only `fastTick` path that evaluates compiled plans directly against snapshot sources when diagnostic allocation is not needed.
`SubstructureRuntimeComposer` resolves registered child definitions into runtime-only composed root definitions so child points, captures, region bounds, scheduler footprints, independent child occurrence deltas/diagnostics, parent ownership/state metadata, child status and opt-in child context propagation into parent contexts, lifecycle callbacks, child occurrence ownership conflicts, and missing/cyclic child diagnostics participate in the tick-driven check lifecycle.
`RuntimeCheckEngineTickDriver` registers that engine on NeoForge server post-tick events, honors a bounded per-tick check budget, uses status-only fast ticks by default, accepts one-shot diagnostic tick requests when full deltas/diagnostics are needed, skips idle no-time ticks, and continues draining queued work across later ticks.
`RuntimeAssetLoader` and `RuntimeAssetJsonLoader` add the first codec-like loading slice for serialized and parsed runtime assets, including deterministic resource ordering, duplicate path diagnostics, serialized predicate descriptor loading, and stable error diagnostics for invalid content.
`RuntimeAssetRepository` indexes the latest valid runtime asset snapshot by asset id and definition id while retaining diagnostics from invalid reload attempts.
`RuntimeAssetReloadListener` and `RuntimeAssetReloadWiring` connect the loader and repository to NeoForge server datapack reloads under `data/*/cumulus/structures/*.json`, preserving the same diagnostic paths as datagen/exported assets.
Runtime assets with embedded definitions now round-trip expanded points, transforms, components, versions, migrations, predicates, captures, and target properties through the JSON loader.
`StructureDefinitionRegistry` and the `Structures.register/resolve` convenience API provide a public definition registry for Java DSL definitions that need to back id-only runtime assets.
`RuntimeAssetEngineBridge` registers embedded definitions, or id-only assets resolved through that definition registry, from the latest valid repository snapshot into the same tick-driven `RuntimeCheckEngine`, and the mod entry point wires that registry-backed bridge through the reload listener's valid-reload hook.
`MinecraftRuntimeInstanceTracker` adds the first production-facing facade for tracking anchored structure instances against `LevelAccessor` snapshot sources, untracking anchors, and marking changed block sections dirty for the tick-driven engine.
`MinecraftRuntimeDiscovery` indexes definitions with explicit `anchor=true` targets first, falls back to required point targets for inferred discovery when no explicit anchor is declared, checks changed blocks against candidate predicates, and tracks matching instances through the Minecraft runtime tracker.
`MinecraftRuntimeEventBridge` connects server block mutation events to that tracker and discovery registry so changed sections are marked dirty automatically for block break, block placement, multi-place, and fluid placement events.
`MinecraftBuildTarget` adds the first production-facing automatic construction adapter for idempotent loaded-world block, region, fluid, capability backing block, optional capability mutation writer hooks, typed energy/item/fluid capability policy routing with built-in idempotent ensure operations, block entity NBT placement, and entity spawning with authored NBT through `LevelAccessor`.
`GhostPreviewCompiler`, `StructureEditorSession`, `StructureEditorController`, and `StructureEditorClientBridge` add an in-game design seed: runtime plans can now produce absolute ghost preview boxes at an editor anchor, track selected elements, compile deterministic render commands, register client key mappings, and render active previews through NeoForge level-render events.

## Definition Layer

`StructureDefinition` remains the public IR root. It now stores:

- expanded `StructurePointDefinition`s;
- component metadata for non-point components such as substructures;
- transform descriptors including mirror and rotation descriptors;
- context derivation descriptors;
- version and migration descriptors;
- diagnostic metadata.

Targets are no longer block-only. `Targets` covers block, block entity, entity, fluid, capability, context, and region targets. Region targets store inclusive relative bounds and support door-like arbitrary rectangular predicates without expanding every block into a separate point.

Components include point, filled box, hollow box, region, and substructure. Region components emit one region point. Substructure components preserve metadata without flattening child definitions, and the in-memory graph resolver can compose nested child definitions while detecting missing children and cycles.

## Runtime Layer

The runtime contracts adopt the important ideas from `design` without reviving its older schema/pass API verbatim.

- `SectionKey` is the section-level partition key for dirty tracking and missing-data requests.
- `DataRequest` and `RequestLedger` keep the rule that the engine requests data but does not force-load chunks.
- `StructureInstanceKey` identifies one structure type at one anchor.
- `RuntimeStructureContext` is versioned by revision and stores derived machine-facing values.
- `InstanceDelta` reports context/status changes to machine logic.
- `StructureClaimIndex` models shared and exclusive part ownership so inter-structure graphs can reject conflicts before runtime logic observes invalid shared state.
- `StructureRuntimePlanCompiler` derives footprint sections, build hints, and visualization elements from `StructureDefinition`.
- `StructureInstanceManager` registers runtime plans, tracks anchored instances, emits prefetch `DataRequest`s for footprints, publishes revisioned runtime-context deltas, and integrates shared/exclusive ownership conflict diagnostics for valid checked instances.
- `RuntimeAssetExporter` turns a runtime plan and `DataDrivenStructureAsset` into stable structured descriptor output and deterministic JSON-like serialized content for the first datagen/runtime-asset slice.
- `StructureCheckPlan` and `CheckPlanCandidate` hold precomputed point/region bounds for each declared transform candidate, so runtime checks can reuse compiled geometry instead of recomputing transform bounds on every tick.
- `StructureCheckExecutor` evaluates descriptor predicates over an in-memory target snapshot or compiled check plan and returns status, diagnostics, visualization, requests, and captured runtime context; its `fastCheck` path reuses the same predicate registry over precompiled candidates and returns lightweight status/counter telemetry without building diagnostics.
- `TargetSnapshotReader` reads definition or compiled-plan targets from a `TargetSnapshotSource`, applies candidate transforms through compiled plan entries, preserves target properties, and leaves missing required data absent so the check executor can request it.
- `MinecraftTargetSnapshotSource` adapts a real loaded `LevelAccessor` to `TargetSnapshotSource` and reads block ids, block entity type ids, entity type ids, fluid ids/properties, registered NeoForge block capability targets, deterministic region block counts, and injected runtime context values.
- `MinecraftCapabilityResolvers` binds descriptor capability names to typed NeoForge `BlockCapability` lookups, includes default energy/item/fluid block resolvers with side context support, and allows custom block capability snapshot properties without forcing every modded capability into the core library.
- `SubstructureGraphResolver` resolves parent/child definition graphs with occurrence ids, offsets, depths, and diagnostics for missing definitions or cycles.
- `StructureContextMigrationExecutor` applies deterministic context-key migration chains and reports missing migration paths with diagnostics.
- `RuntimeAssetDatagenProvider` exports ordered runtime asset sets and rejects duplicate resource paths before generation.
- `BuildPlanExecutor` turns runtime entries into deterministic point/region build actions, preserves target properties needed for construction metadata, rejects unsupported target-specific actions before mutation, and applies block/region/capability mutation actions idempotently to an in-memory build target.
- `RuntimeScheduler` registers anchored plans, indexes footprint sections, schedules only dirty impacted instances, honors per-frame check budgets, carries pending work across frames, unregisters queued instances, and records emitted data requests through `RequestLedger`.
- `RuntimeCheckEngine` registers definitions, resolves substructure graphs for tracked roots, composes child points at cumulative child anchor offsets into runtime-only definitions, compiles reusable check plans for tracked instances, tracks anchored sources, marks dirty sections, drains checks under a budget, publishes root plus child occurrence `InstanceDelta`s, detects independent child occurrence ownership conflicts, returns `RuntimeFrameResult` deltas/requests using the same check executor as in-memory and Minecraft-backed sources, and can drain status-only fast checks with `RuntimeFastCheck` telemetry.
- `SubstructureRuntimeComposer` prefixes child occurrence point ids/captures/groups, translates child point and region bounds by cumulative substructure offsets, preserves root metadata, exposes root-relative runtime child occurrence metadata, and lets unresolved missing/cyclic substructure graphs publish invalid preflight diagnostics before snapshot reads.
- `SubstructureLifecycleState` maps composed parent check results into independent child occurrence contexts and diagnostics with child-local capture names, child anchors, parent occurrence ownership metadata, propagated parent status, child status entries in the parent context, optional child context snapshots under parent context keys, and child-relative diagnostic evidence; `RuntimeCheckEngine` publishes lifecycle callbacks for added child occurrences and status changes, while `SubstructureClaimState` claims child occurrence anchors by owning parent and invalidates conflicting child deltas with ownership diagnostics.
- `RuntimeCheckEngineTickDriver` registers on `ServerTickEvent.Post`, drives the runtime engine with a positive per-tick budget, defaults to `fastTick` status telemetry, can run the next tick through the diagnostic `tick` path on request, keeps recent frame results, and is held by the mod entry point for the mod lifetime.
- `RuntimeAssetLoader` consumes parsed map/list asset content produced by datapack or datagen paths, while `RuntimeAssetJsonLoader` parses the exporter JSON string form before handing it to the same loader; both report invalid content through `DiagnosticGraph`.
- `RuntimeAssetRepository` exposes immutable snapshots of loaded assets by runtime asset id and definition id, replacing the active snapshot only after a valid reload.
- `RuntimeAssetReloadListener` scans NeoForge server resources below `cumulus/structures`, loads JSON runtime assets off-thread, applies valid snapshots into the repository on reload completion, and is registered by `RuntimeAssetReloadWiring` from the mod entry point.
- `StructureDefinitionRegistry` stores Java DSL definitions by id for id-only datapack assets; the global `Structures` registry is used by the default mod entry point.
- `RuntimeAssetEngineBridge` consumes the latest valid repository snapshot, registers assets that include embedded definitions or registry-resolved id-only definitions into `RuntimeCheckEngine`, and is invoked by the production reload hook after repository update.
- `MinecraftRuntimeInstanceTracker` registers anchored runtime instances with Minecraft-backed snapshot sources, untracks anchors, and bridges changed block positions into dirty section marks for scheduled checks.
- `MinecraftRuntimeDiscovery` registers definitions with explicit anchor target metadata or inferred required-point candidates, discovers matching changed blocks from Minecraft target readers, computes the owning structure anchor from the matched point offset, and tracks the instance.
- `MinecraftRuntimeEventBridge` listens for NeoForge block mutation events and forwards changed block positions into the tracker and discovery registry, including multi-block placements.
- `MinecraftBuildTarget` adapts `BuildPlanExecutor` to real loaded worlds for block, region, fluid, capability backing block, capability mutation actions through an optional `CapabilityBuildWriter`, typed energy/item/fluid capability mutation policy routing through `MinecraftCapabilityBuildWriters`, concrete idempotent `ensure_energy`, `ensure_item`, and `ensure_fluid` typed writers for NeoForge handlers, block entity NBT placement, and entity spawning with authored NBT, explicitly reports unsupported capability mutation actions without a capability writer, resolves registry block/fluid/block entity/entity ids, skips unknown/unsupported targets, and preserves idempotent writes.
- `GhostPreviewCompiler` translates runtime plan entries into absolute ghost preview boxes with target metadata, `StructureEditorSession` keeps immutable editor anchor/selection state, `StructureEditorController` owns the active session and render plan, and `StructureEditorClientBridge` registers client key mappings plus level-render event hooks for ghost preview line-box rendering.

Later runtime plans should compile `StructureDefinition` into fast and diagnostic check plans instead of reintroducing `ArchitectSchema` as the author-facing type.

## Diagnostics And Tooling

The current architecture upgrades `design`'s flat diagnostic records into `DiagnosticGraph`.

- `DiagnosticNode` carries stable code/id, severity, message/localization key, evidence, source path, and fix hints.
- `DiagnosticEdge` expresses nested causes such as rule -> predicate -> evidence.
- `DiagnosticEvidence` preserves target, predicate, component path, relative position, unloaded/missing-data details, and structured descriptors.
- `FixHint` is the typed seed for automatic construction and repair guidance.
- `VisualizationModel` and `VisualizationElement` expose overlay/debug data for future in-game design and visual editors.
- `GhostPreviewModel` and `GhostPreviewElement` expose concrete absolute preview geometry for in-game ghost rendering without coupling the backend to a client screen.
- `DataDrivenStructureAsset` and `StructureDatagenSpec` provide stable resource paths for runtime/debug asset generation.
- `PredicateRegistry` and `PredicateDescriptors.fromSerialized` provide the first serialized predicate loading path for custom predicate execution.

## Issue 36 Coverage

- Visualization design: covered by `VisualizationModel` contracts, concrete ghost preview models, render-command compilation, and client level-render ghost preview wiring.
- Automatic construction: covered by structured `FixHint`, an in-memory point/region/capability-backed/capability-mutation build action executor, and a conservative loaded-world `LevelAccessor` block/region/fluid/capability backing block/typed energy-item-fluid capability mutation policy with built-in idempotent ensure operations/block entity NBT/entity spawning build target with explicit unsupported diagnostics when no mod-specific capability writer is supplied.
- High-performance runtime: covered by `SectionKey`, `DataRequest`, `RequestLedger`, runtime context, ownership contracts, a budgeted dirty-section scheduler, a scheduler-to-check lifecycle engine, NeoForge server post-tick driver registration, compiled-plan fast check evaluation with valid-candidate short-circuiting, status-only runtime fast-tick telemetry, and automatic fast/default versus diagnostic/on-request tick policy.
- Production tracking: covered by a Minecraft runtime instance tracking facade that attaches `LevelAccessor` snapshot sources to the tick-driven engine, maps changed block positions to dirty sections, wires NeoForge block mutation events into dirty-section marks, and discovers instances using explicit anchor metadata or inferred required-point candidates.
- Data driven/datagen: covered by asset descriptors, stable resource paths, deterministic descriptor content, JSON serialization, embedded-definition round-tripping, export-set planning, runtime asset loaders with diagnostics, runtime asset repository indexing, NeoForge server datapack reload listener registration, and valid-reload registration of embedded or registry-resolved id-only definitions into the tick-driven runtime engine.
- Multi-level error analysis: covered by `DiagnosticGraph`.
- Non-block predicates: covered by non-block target descriptors.
- Non-block checks: covered in the executor slice for block entity, entity, fluid, capability, and context targets, with both world-agnostic and loaded-`LevelAccessor` source adapters plus registered NeoForge block capability lookup.
- Arbitrary rectangular predicates: covered by region target, region component, and in-memory region check execution.
- Substructures: covered by substructure component descriptors, in-memory graph resolution with missing-child/cycle diagnostics, tick-driven runtime composition that checks child points/regions at child anchor offsets before publishing parent validity, independent child occurrence deltas/diagnostics plus parent ownership/status metadata for lifecycle observers, child status propagation and opt-in child context snapshots back into parent contexts, lifecycle callbacks for child occurrence additions/status changes, and child occurrence ownership conflict diagnostics.
- Runtime context conversion: covered by context derivation descriptors, `RuntimeStructureContext`, and executable context migration.
- In-game design/editing: covered by visualization/debug model seeds, ghost preview compilation, immutable editor anchor/selection state, active editor controller state, selection key handling, and client ghost preview renderer wiring.
- Upgrade compatibility: covered by version and migration descriptors plus deterministic context migration execution.
- Rotation/geometry actions: covered by transform descriptors for mirror and rotations, runtime-plan transform helpers, compiled transform-aware check plans, and in-memory check candidate evaluation for simple Y rotations/mirror.
- Shared/exclusive control: covered by `StructureClaimIndex` and `StructureInstanceManager` conflict diagnostics for checked instances.

## Current Execution Limits

The executable slice is intentionally conservative. It does not yet include a polished editor screen, persistence workflow, or network protocol for sharing authored in-game edits; the current client editor wiring focuses on active ghost preview sessions, selection controls, and renderer hooks.

## Migration From Previous Designs

Keep from `design2`:

- immutable IR records;
- descriptor-first target/predicate/transform model;
- Java DSL entry point;
- deterministic component expansion;
- definition-time validation.

Keep from `design`:

- section partitioning;
- request-not-load missing data boundary;
- runtime contexts and instance deltas;
- diagnostics with evidence and fix hints;
- cross-structure dependency/conflict concepts.

Do not carry forward verbatim:

- `ArchitectSchema` as the author-facing API;
- block-only `PartKind`/`MatchSpec` boundaries;
- object-handle `CustomPartRef`;
- flat diagnostic lists as the only diagnostic representation;
- translation-only transform as the primary geometry model.
