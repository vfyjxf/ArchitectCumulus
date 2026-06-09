# ArchitectCumulus Implementation Map

This file points to the actual code implementation for the current branch.

## Public API

Definition DSL and author-facing contracts:

- `src/main/java/dev/vfyjxf/cumulus/api/definition/Structures.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/StructureDefinition.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionBuilder.java`
- `src/main/java/dev/vfyjxf/cumulus/api/component/Components.java`
- `src/main/java/dev/vfyjxf/cumulus/api/target/Targets.java`
- `src/main/java/dev/vfyjxf/cumulus/api/predicate/PredicateDescriptors.java`
- `src/main/java/dev/vfyjxf/cumulus/api/transform/Transforms.java`

Runtime-facing API contracts:

- `src/main/java/dev/vfyjxf/cumulus/api/runtime/RuntimeStructureContext.java`
- `src/main/java/dev/vfyjxf/cumulus/api/runtime/InstanceDelta.java`
- `src/main/java/dev/vfyjxf/cumulus/api/runtime/StructureInstanceKey.java`
- `src/main/java/dev/vfyjxf/cumulus/api/request/DataRequest.java`
- `src/main/java/dev/vfyjxf/cumulus/api/partition/SectionKey.java`

## Core Implementation

Runtime plan compilation and asset export:

- `src/main/java/dev/vfyjxf/cumulus/impl/StructureRuntimePlanCompiler.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/StructureRuntimePlan.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/RuntimePlanEntry.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/RuntimeAssetExporter.java`

Check execution:

- `src/main/java/dev/vfyjxf/cumulus/impl/check/StructureCheckExecutor.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/check/StructureCheckPlan.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/check/TargetSnapshotReader.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/check/TargetSnapshotSource.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/check/FastCheckResult.java`

Runtime scheduler and engine:

- `src/main/java/dev/vfyjxf/cumulus/impl/runtime/RuntimeScheduler.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/runtime/RuntimeCheckEngine.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/runtime/RuntimeCheckEngineTickDriver.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/runtime/RuntimeFrameResult.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/runtime/RuntimeFastCheck.java`

Substructures:

- `src/main/java/dev/vfyjxf/cumulus/impl/substructure/SubstructureGraphResolver.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/substructure/SubstructureRuntimeComposer.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/runtime/SubstructureLifecycleState.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/runtime/SubstructureLifecycleEvent.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/runtime/SubstructureClaimState.java`

Datagen and runtime asset loading:

- `src/main/java/dev/vfyjxf/cumulus/impl/datagen/RuntimeAssetDatagenProvider.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/codec/RuntimeAssetLoader.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/codec/RuntimeAssetJsonLoader.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/codec/RuntimeAssetRepository.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/codec/RuntimeAssetReloadListener.java`

Automatic build:

- `src/main/java/dev/vfyjxf/cumulus/impl/build/BuildPlanExecutor.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/build/InMemoryBuildTarget.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/minecraft/MinecraftBuildTarget.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/minecraft/MinecraftCapabilityBuildWriters.java`

Minecraft runtime integration:

- `src/main/java/dev/vfyjxf/cumulus/impl/minecraft/MinecraftTargetSnapshotSource.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/minecraft/MinecraftRuntimeInstanceTracker.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/minecraft/MinecraftRuntimeDiscovery.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/minecraft/MinecraftRuntimeEventBridge.java`

In-game editor and ghost preview:

- `src/main/java/dev/vfyjxf/cumulus/impl/editor/GhostPreviewCompiler.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/editor/StructureEditorSession.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/editor/StructureEditorController.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/editor/GhostPreviewRenderPlan.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/editor/GhostPreviewRenderer.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/editor/StructureEditorClientBridge.java`

Mod entry wiring:

- `src/main/java/dev/vfyjxf/cumulus/Cumulus.java`

## Tests Showing Behavior

- `src/test/java/dev/vfyjxf/cumulus/api/runtime/DefinitionExpansionTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/check/StructureCheckExecutorTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/runtime/RuntimeCheckEngineTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/minecraft/MinecraftBuildTargetTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/editor/GhostPreviewCompilerTest.java`

## Current File Count

The current implementation lives mainly under:

- `src/main/java/dev/vfyjxf/cumulus/impl`
- `src/test/java/dev/vfyjxf/cumulus/impl`

There are dozens of Java implementation files there; this is the branch's formal implementation, not a separate experimental package.
