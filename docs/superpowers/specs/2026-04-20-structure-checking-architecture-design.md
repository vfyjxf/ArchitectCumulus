# ArchitectCumulus Structure Checking Architecture Design

Date: 2026-04-20

## Purpose

ArchitectCumulus is a NeoForge/Minecraft library for defining, validating, checking, diagnosing, and managing flexible multiblock-style structures.

The library should feel natural to Minecraft mod developers: public APIs may use `BlockPos`, `Direction`, `LevelAccessor`, `BlockState`, `Entity`, `BlockEntity`, `ResourceLocation`, datagen providers, GameTest, and NeoForge registries. Internally, the design must stay generalized enough that a "structure point" is not limited to a single block. The same model should support blocks, block entities, entities, fluids, regions, external state, context predicates, and inter-structure dependencies.

The first implementation should prioritize clear APIs, reliable diagnostics, and normal machine-scale multiblocks. The architecture must leave a direct path to larger modular structures with chunk/section indexing, local invalidation, and budgeted checking.

## Confirmed Direction

- Use a NeoForge/Minecraft-specialized API surface.
- Keep the internal model generic and extensible.
- Use Java code as the authoritative definition source.
- Provide Java DSLs for structure definitions, components, predicates, and datagen.
- Generate runtime/debug data from code definitions.
- Support both fast runtime checks and full diagnostic checks.
- Target small and medium structures first, while supporting larger component-based structures.
- Support both internal structure context and inter-structure runtime context.
- Support explicit coordinate templates and composable structure components.
- Use pluggable transform sets.
- Use a unified predicate core with typed Minecraft-friendly DSLs.
- Provide a standard `StructureInstanceManager`.
- Build a complete diagnostic graph and project it into developer, player, visualization, and build-validation reports.
- Provide a common standard implementation while keeping extension points open.

## Architecture Overview

The recommended architecture is a layered compilation pipeline:

```text
Java DSL definitions
  -> Structure IR
  -> generated runtime/debug assets
  -> runtime StructureRegistry
  -> compiled FastCheckPlan and DiagnosticPlan
  -> StructureInstanceManager
  -> match results, diagnostics, and runtime structure contexts
```

The central design choice is that the DSL does not directly become runtime logic. Definitions are expanded into a stable intermediate representation (IR). Datagen, runtime plan compilation, diagnostics, visualization, and tests all consume the same IR.

This makes the standard implementation easy to use while preserving extension points for custom targets, predicates, components, transforms, diagnostics, and runtime policies.

## Layers

### Definition Layer

The Definition Layer is the author-facing Java API. It supports:

- structure ids and metadata;
- coordinate-template points;
- component-composed structures;
- typed target DSLs for blocks, block entities, entities, fluids, regions, and context targets;
- predicate chains;
- named captures;
- context derivation rules;
- allowed transform sets;
- check policies;
- diagnostic metadata;
- datagen registration.

The API may use Minecraft idioms directly, especially where they make authoring simpler: `BlockPos`, `Direction`, block tags, entity types, block state properties, capabilities, `ResourceLocation`, and NeoForge registries.

### IR Layer

The IR is the shared representation after DSL expansion. It should describe structure logic without depending on fluent builder state or Java lambdas.

The IR contains:

- `StructureType` metadata;
- expanded `StructurePoint` records;
- target type ids;
- predicate node ids and serialized standard predicate data;
- runtime-only predicate registry ids;
- allowed transform set ids;
- component source metadata;
- relative coordinates and location expressions;
- required/optional/grouped point metadata;
- capture names;
- context dependency declarations;
- diagnostic tags, severity defaults, and localization keys;
- version/schema information.

The IR is not required to be perfectly game-agnostic. It can contain Minecraft-specific target ids and predicate ids. The important boundary is that the check engine sees a uniform model instead of hardcoded "all checks are block checks" assumptions.

### Standard Extensions Layer

ArchitectCumulus should ship a complete standard implementation for common use cases:

- targets: block, block entity, entity, fluid, region, context;
- predicates: exact block, block tag, state property, block entity type, capability availability, entity type, entity distance, fluid type, region loaded, custom registry predicate;
- components: point, line, box, hollow box, layer, ring, port, repeated component, optional component, mirrored component, named group;
- transforms: identity, horizontal facing, horizontal mirror, common 3D rotations, custom transform set;
- check policies: standard machine, lazy machine, diagnostic-heavy development policy;
- diagnostic projectors: developer log, player message, visualization model, datagen validation;
- runtime management: standard instance manager and invalidation index.

Advanced mods can register new targets, predicates, components, transforms, diagnostic projectors, and runtime policies.

### Compilation Layer

The Compilation Layer turns IR into executable plans:

- `FastCheckPlan` for short-circuit runtime validation;
- `DiagnosticPlan` for full failure collection and explanation.

Compilation responsibilities:

- pre-expand or index transforms;
- group required and optional points;
- build relative-coordinate and bounding-box indexes;
- build chunk/section invalidation indexes;
- order predicates by estimated cost;
- track target and event dependencies;
- create cache keys;
- build context dependency graphs;
- preserve diagnostic source paths;
- prepare candidate transform evaluation.

The standard compiler should be deterministic so generated plans and diagnostics are stable across runs.

### Runtime Layer

The Runtime Layer provides two levels of API:

- a direct checker for explicit calls;
- a standard `StructureInstanceManager` for normal machines.

The direct checker accepts a structure type or compiled plan, world access, anchor, transform candidates, and optional existing context. It returns a fast result or diagnostic graph depending on mode.

The standard instance manager handles:

- controller/anchor registration;
- structure type association;
- cached `StructureInstanceState`;
- block, block entity, entity, chunk load/unload, and neighbor structure changes;
- mapping world changes to affected structure points;
- immediate checks for critical changes;
- medium-frequency local checks;
- low-frequency full checks;
- valid, invalid, and stale context states;
- runtime context revisions;
- inter-structure graph resolution hooks.

### Tooling Layer

Tooling consumes the same IR and diagnostic output:

- datagen providers;
- build-time validation;
- NeoForge GameTest structures;
- JUnit assertions;
- developer logs;
- player-facing localized messages;
- debug overlay models;
- future ghost previews or structure inspectors.

The first architecture spec only defines a stable visualization output model. A full client UI can be designed later.

## Core Model

### `StructureType`

`StructureType` is a structure blueprint. It owns:

- id and version metadata;
- component tree;
- allowed transform sets;
- target and predicate declarations;
- context schema;
- check policy;
- diagnostic policy;
- datagen/debug metadata.

### `StructureComponent`

`StructureComponent` is a generator of structure content. Components may produce:

- structure points;
- point groups;
- constraints;
- captures;
- context rules;
- diagnostic metadata.

Standard components include point, box, hollow box, layer, ring, port, repeat, optional, mirror, and named group.

### `StructurePoint`

`StructurePoint` is the smallest compiled check unit. A point may target a block at a relative coordinate, but it may also target an entity query, block entity, fluid, region, or context condition.

Fields should include:

- stable point id;
- source component id/path;
- relative location or query expression;
- target type;
- predicate chain;
- required/optional status;
- capture behavior;
- group tags;
- diagnostic labels and severity defaults.

### `StructurePredicate<TTarget, TContext>`

`StructurePredicate` is the unified predicate contract. Standard typed DSLs should compile down to this model.

Predicate implementations should support:

- fast boolean checks;
- optional diagnostic explanation;
- dependency declaration for invalidation;
- cache-key contribution;
- optional serialization;
- registry id for runtime-only predicates.

Predicate traits:

- `diagnosticAware`: can explain failures.
- `indexAware`: can declare affected world regions or events.
- `cacheAware`: contributes to cache keys.
- `runtimeOnly`: restored by registry id instead of serialized data.

### `StructureContext`

`StructureContext` exists in two forms:

- `MatchContext`: mutable, short-lived, used during matching and diagnosis.
- `RuntimeStructureContext`: stable, versioned, exposed to machines after successful or partial resolution.

The context includes:

- anchor;
- transform;
- captures;
- derived facing;
- dimensions;
- ports;
- controller target;
- matched block entities or entities;
- local structure properties;
- dependency declarations;
- revision number.

### `StructureGraphResolver`

Inter-structure dependencies should not be forced into a single structure matcher. A matched structure produces a runtime context. Then `StructureGraphResolver` resolves:

- parent/child structures;
- ports and adjacency;
- shared parts;
- dependency state;
- conflicts;
- cross-structure runtime views.

This keeps single-structure matching focused and prevents complex machines from turning into one oversized matcher.

## Example API Direction

The final syntax can change, but the API should support this style:

```java
StructureType blastFurnace = Structures.define(id("blast_furnace"))
    .transforms(Transforms.horizontalFacing(), Transforms.mirrorX())
    .component(Components.box("casing", from(-1, 0, -1), to(1, 2, 1))
        .where(BlockPredicates.tag(ModTags.HEAT_RESISTANT_CASING)))
    .point("controller", at(0, 1, -1), Targets.block()
        .where(BlockPredicates.block(ModBlocks.CONTROLLER))
        .capture("controller"))
    .point("input_port", at(-1, 1, 0), Targets.blockEntity()
        .where(BlockEntityPredicates.hasItemHandler())
        .capture("input"))
    .context(ctx -> ctx.requireCaptured("controller").deriveFacingFromController())
    .checkPolicy(CheckPolicies.standardMachine())
    .build();
```

This illustrates the intended boundaries:

- `StructureType` defines the blueprint.
- Components generate points and metadata.
- Targets select what is queried.
- Predicates validate targets.
- Captures and context rules create runtime data.
- Policies control checking and diagnostics.

## Data Generation

Java definitions are the source of truth. Datagen expands them into two asset categories.

### Runtime Assets

Runtime assets contain what the server needs to load and compile plans:

- structure id and schema version;
- expanded points;
- target type ids;
- serializable predicate data;
- runtime-only predicate registry ids;
- transform set ids;
- context schema;
- check policy id/config;
- validation metadata.

### Debug Assets

Debug assets contain extra information for diagnostics and tooling:

- component source paths;
- author-facing names;
- comments or descriptions;
- localization keys;
- visual grouping;
- colors or severity hints;
- source DSL labels;
- validation hints.

Runtime assets must remain sufficient for checks. Debug assets improve error reporting and visualization but should not be mandatory for normal server operation.

## Runtime Checking Strategy

Runtime checking supports three trigger classes:

### Immediate Trigger

Used when the structure is likely to have changed materially:

- controller placed or removed;
- explicit player assembly action;
- critical block changed;
- chunk reload affecting anchor or controller;
- manual API call.

### Medium Local Check

Used for normal runtime invalidation. The manager maps world changes through chunk/section and point indexes, then rechecks affected groups or candidate transforms.

### Low Full Check

Used occasionally to correct missed events, entity movement, chunk loading edge cases, external dependencies, or stale contexts.

## Cache Model

The standard implementation should include:

- `PlanCache`: structure type to compiled plans.
- `TransformCandidateCache`: anchor to possible transforms.
- `PointResultCache`: point to last target/predicate result.
- `ContextCache`: latest runtime structure context.
- `DependencyCache`: inter-structure links, ports, shared parts, and dependency status.

All exposed runtime contexts should carry a revision so machine logic can detect changes without deep comparison.

## Diagnostics

Diagnostic execution produces a `DiagnosticGraph`, not only a list of strings.

The graph records:

- checked structure type;
- anchor and transform candidate;
- point results;
- actual world targets;
- predicate pass/fail state;
- missing targets;
- wrong target types;
- context capture failures;
- context derivation failures;
- transform rejection;
- component conflicts;
- inter-structure dependency failures;
- shared part conflicts;
- unloaded areas;
- runtime-only predicate availability;
- severity;
- source component;
- relative and absolute positions;
- related entity ids or structure ids;
- visualization hints.

Standard error categories:

- `MISSING_TARGET`
- `WRONG_TARGET_TYPE`
- `PREDICATE_FAILED`
- `CONTEXT_CAPTURE_FAILED`
- `CONTEXT_DERIVATION_FAILED`
- `TRANSFORM_REJECTED`
- `COMPONENT_CONFLICT`
- `DEPENDENCY_UNRESOLVED`
- `SHARED_PART_CONFLICT`
- `UNLOADED_AREA`
- `RUNTIME_ONLY_UNAVAILABLE`

## Diagnostic Projections

The same graph can be projected into:

- developer reports for logs and tests;
- player reports with localized short messages;
- visualization models for overlays or future tools;
- datagen/build validation errors;
- GameTest assertions.

The visualization model should include stable positions, groups, bounding boxes, severity, display priority, and localization keys. A full visual UI is out of scope for the first implementation plan.

## Package Direction

Suggested package layout:

```text
dev.vfyjxf.cumulus
  api.definition
  api.component
  api.predicate
  api.target
  api.context
  api.transform
  api.diagnostic
  api.runtime
  api.datagen

  impl.definition
  impl.compile
  impl.runtime
  impl.diagnostic
  impl.serialization
  impl.index
  impl.minecraft

  registry
```

The implementation package root is `dev.vfyjxf.cumulus`. The first implementation plan must delete the current example template package `com.example.examplemod` and create the library structure directly under this root.

The mod id should be finalized in the implementation plan. Until a better id is chosen, use `cumulus` as the working mod id and update the current template value `examplemod` during project cleanup.

## Testing Strategy

Testing should use NeoForge's testing stack as a first-class part of the design, plus plain JUnit for fast core validation.

### Plain JUnit Tests

Plain JUnit should not start Minecraft. Use it for:

- IR validation;
- coordinate transforms;
- component expansion;
- predicate composition behavior that does not need registries;
- diagnostic graph projection;
- serialization schema checks;
- cache-key stability.

### NeoForge JUnit Extension / Mod Test Harness

Use NeoForge's JUnit extension or mod test harness for controlled mod-environment tests.

Use it for:

- registry integration;
- `ResourceLocation` and registry-backed predicates;
- datagen provider integration;
- reload listener behavior;
- standard Minecraft target adapters;
- predicate behavior that needs Minecraft registries but not a full placed test world.

### NeoForge GameTest

Use NeoForge GameTest for real world behavior:

- placed multiblock structures;
- blocks, block entities, fluids, and entities;
- controller lifecycle;
- chunk/section invalidation;
- local and full rechecks;
- runtime context generation;
- player-facing diagnostic behavior;
- inter-structure dependencies.

### Datagen Validation Tests

Datagen validation can be driven by JUnit or Gradle tasks. It should verify:

- generated resources are stable;
- illegal definitions fail during build;
- errors point to component/source metadata;
- runtime-only predicates have registry ids;
- runtime-only predicates define fallback diagnostic behavior where needed.

## Follow-Up Specs

This architecture should be implemented through smaller specs in this order:

1. Core IR and Definition DSL
2. Predicate and Target System
3. Plan Compiler and Check Execution
4. Runtime Instance Manager and Invalidation
5. Diagnostics and Visualization Model
6. Datagen and Build Validation
7. Structure Graph / Inter-Structure Dependencies

The first implementation plan should start with "Core IR and Definition DSL", because every later layer depends on stable structure definitions and expansion semantics.

## Non-Goals For The First Implementation Plan

- Full client visualization UI.
- Arbitrary async structure solving.
- Large cross-chunk dynamic network engine.
- Player-authored JSON for every advanced predicate.
- Perfect game-agnostic API surface.
- Deep lifecycle ownership of every machine implementation.

These can be revisited after the core IR, predicate system, compiler, and manager boundaries are proven.
