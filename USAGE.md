# ArchitectCumulus Usage

This file gives root-level usage examples for the current branch.

## Define A Structure

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

## Register And Track Runtime Checks

```java
var engine = new RuntimeCheckEngine()
    .register(definition)
    .track(definition.id(), anchorBlockPos, targetSnapshotSource);

engine.markDirty(SectionKey.fromBlock(anchorBlockPos));
var frame = engine.tick(serverTick, 8);
```

Use `fastTick` when you only need status telemetry and want to avoid diagnostic allocation:

```java
var fastFrame = engine.fastTick(serverTick, 8);
```

## Use Substructure Lifecycle Sync

```java
var engine = new RuntimeCheckEngine()
    .substructureSyncPolicy(SubstructureSyncPolicy.STATUS_AND_CONTEXT)
    .addSubstructureLifecycleListener(event -> {
        // event.transition(), event.previousStatus(), event.currentStatus()
    });
```

With `STATUS_AND_CONTEXT`, parent contexts receive keys like:

```text
substructure.<occurrenceId>.status
substructure.<occurrenceId>.anchor_offset
substructure.<occurrenceId>.context.<childCapture>
```

## Compile Ghost Preview And Client Render Commands

```java
var plan = StructureRuntimePlanCompiler.compile(definition);
var session = StructureEditorSession.start(plan, anchorBlockPos)
    .select("controller");

var preview = session.preview();
var renderPlan = GhostPreviewRenderPlan.fromPreview(preview);
```

The mod entry point wires `StructureEditorClientBridge` on the physical client. It registers editor key mappings and renders the active ghost preview through NeoForge level-render events.

## Most Useful Tests To Read

- `src/test/java/dev/vfyjxf/cumulus/api/runtime/DefinitionExpansionTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/check/StructureCheckExecutorTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/runtime/RuntimeCheckEngineTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/minecraft/MinecraftBuildTargetTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/editor/GhostPreviewCompilerTest.java`

## Full Design

Read `ARCHITECTURE.md` first, then the full spec:

- `docs/superpowers/specs/2026-06-08-architect-cumulus-architecture.md`
