# ArchitectCumulus Design3

This is the root-level design entry point for the current `design3` branch.

For the full architecture document, see:

- `docs/superpowers/specs/2026-06-08-architect-cumulus-design3.md`

## What Design3 Combines

Design3 combines the definition-first work from `design2` with the runtime engine ideas from `design`.

- `design2`: Java DSL, immutable structure definitions, targets, predicates, transforms, context descriptors, validation, and datagen-facing descriptors.
- `design`: runtime scheduling, dirty-section checks, request-not-load behavior, diagnostics, instance deltas, ownership conflicts, and high-performance runtime concepts.
- AnvilLib issue 36: visualization, automatic construction, data-driven structures, non-block checks, rectangular predicates, substructures, runtime context conversion, migration, rotations, shared/exclusive parts, and in-game design support.

## Main Layers

```text
Java DSL definitions
  -> immutable StructureDefinition IR
  -> runtime/debug/datagen assets
  -> compiled check and diagnostic plans
  -> RuntimeCheckEngine / scheduler
  -> RuntimeStructureContext, diagnostics, visualization, editor previews
```

## Implemented Design3 Areas

- Java definition DSL for structures, points, regions, boxes, and substructures.
- Block, block entity, entity, fluid, capability, region, and context targets.
- Predicate registry and serialized predicate loading.
- Transform-aware check plans and fast status-only runtime checks.
- Dirty-section runtime scheduler and NeoForge server tick driver.
- Runtime asset export/load/repository/reload wiring.
- Minecraft runtime tracking/discovery/event bridge.
- Automatic build actions and Minecraft build target support.
- Substructure composition, child deltas, lifecycle callbacks, context sync, diagnostics, and ownership conflict detection.
- Ghost preview compiler, editor controller, render commands, client key mappings, and NeoForge level-render ghost preview wiring.

## Current Conservative Limits

The design3 implementation now has a working backend/runtime/client preview seed. It does not yet include a polished editor screen, persisted in-game editing workflow, or network protocol for sharing authored edits.

## Best Code Entry Points

- `src/main/java/dev/vfyjxf/cumulus/api/definition/Structures.java`
- `src/main/java/dev/vfyjxf/cumulus/api/component/Components.java`
- `src/main/java/dev/vfyjxf/cumulus/api/target/Targets.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/RuntimeCheckEngine.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/minecraft/MinecraftBuildTarget.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/editor/StructureEditorController.java`

For usage examples, start with `USAGE.md`.
