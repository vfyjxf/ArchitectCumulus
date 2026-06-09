# ArchitectCumulus Design3 Implementation Map

This file points to the actual code implementation for the current `design3` branch.

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

## Core Design3 Implementation

Runtime plan compilation and asset export:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3/StructureRuntimePlanCompiler.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/StructureRuntimePlan.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/RuntimePlanEntry.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/RuntimeAssetExporter.java`

Check execution:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3/check/StructureCheckExecutor.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/check/StructureCheckPlan.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/check/TargetSnapshotReader.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/check/TargetSnapshotSource.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/check/FastCheckResult.java`

Runtime scheduler and engine:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/RuntimeScheduler.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/RuntimeCheckEngine.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/RuntimeCheckEngineTickDriver.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/RuntimeFrameResult.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/RuntimeFastCheck.java`

Substructures:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3/substructure/SubstructureGraphResolver.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/substructure/SubstructureRuntimeComposer.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/SubstructureLifecycleState.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/SubstructureLifecycleEvent.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/runtime/SubstructureClaimState.java`

Datagen and runtime asset loading:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3/datagen/RuntimeAssetDatagenProvider.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/codec/RuntimeAssetLoader.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/codec/RuntimeAssetJsonLoader.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/codec/RuntimeAssetRepository.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/codec/RuntimeAssetReloadListener.java`

Automatic build:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3/build/BuildPlanExecutor.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/build/InMemoryBuildTarget.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/minecraft/MinecraftBuildTarget.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/minecraft/MinecraftCapabilityBuildWriters.java`

Minecraft runtime integration:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3/minecraft/MinecraftTargetSnapshotSource.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/minecraft/MinecraftRuntimeInstanceTracker.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/minecraft/MinecraftRuntimeDiscovery.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/minecraft/MinecraftRuntimeEventBridge.java`

In-game editor and ghost preview:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3/editor/GhostPreviewCompiler.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/editor/StructureEditorSession.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/editor/StructureEditorController.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/editor/GhostPreviewRenderPlan.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/editor/GhostPreviewRenderer.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/design3/editor/StructureEditorClientBridge.java`

Mod entry wiring:

- `src/main/java/dev/vfyjxf/cumulus/Cumulus.java`

## Tests Showing Behavior

- `src/test/java/dev/vfyjxf/cumulus/api/design3/DefinitionExpansionTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/design3/check/StructureCheckExecutorTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/design3/runtime/RuntimeCheckEngineTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/design3/minecraft/MinecraftBuildTargetTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/design3/editor/GhostPreviewCompilerTest.java`

## Current File Count

The design3 implementation lives mainly under:

- `src/main/java/dev/vfyjxf/cumulus/impl/design3`
- `src/test/java/dev/vfyjxf/cumulus/impl/design3`

There are dozens of Java implementation files there; this is not just a design document branch.
