# Core IR and Definition DSL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working ArchitectCumulus foundation: project identity cleanup, the in-memory structure IR, component expansion, and a Java definition DSL with plain JUnit, NeoForge JUnit, and GameTest coverage.

**Architecture:** This first slice stops before runtime checking. Java DSL builders compile into immutable IR records under `dev.vfyjxf.cumulus`; components expand into `StructurePointDefinition`s; validation catches definition-time conflicts. Later predicate execution, plan compilation, diagnostics, datagen serialization, and runtime managers will consume this IR.

**Tech Stack:** Java 21, NeoForge 21.1.222, ModDevGradle 2.0.141, JUnit Jupiter, `net.neoforged:testframework`, NeoForge GameTest.

---

## References

- Architecture spec: `docs/superpowers/specs/2026-04-20-structure-checking-architecture-design.md`
- NeoForge 1.21.1 GameTest docs: https://docs.neoforged.net/docs/1.21.1/misc/gametest/
- Local NeoForge test framework jar confirmed at `~/.gradle/caches/modules-2/files-2.1/net.neoforged/testframework/21.1.222/.../testframework-21.1.222.jar`
- Local test framework classes confirmed: `net.neoforged.testframework.junit.EphemeralTestServerProvider`, `net.neoforged.testframework.gametest.EmptyTemplate`

## Scope

In scope:

- Delete example template code instead of migrating it.
- Set project identity to package root `dev.vfyjxf.cumulus` and working mod id `cumulus`.
- Add a minimal mod entrypoint.
- Add JUnit and NeoForge test framework dependencies.
- Create immutable IR records for definitions, points, targets, predicates, transforms, context rule descriptors, and diagnostic metadata.
- Create component expansion for direct point, filled box, and hollow box.
- Create Java DSL entry points sufficient for normal structure definition authoring.
- Add validation for duplicate ids, duplicate captures, invalid boxes, and required spatial conflicts.
- Add pure JUnit tests, NeoForge JUnit extension tests, and a GameTest smoke test.

Out of scope:

- Runtime world checking.
- Predicate evaluation against `Level`.
- Serialization/datagen output.
- Full diagnostics graph.
- Runtime instance manager and invalidation indexes.
- Inter-structure dependency resolution.

## Locked Decisions

- The template package `com.example.examplemod` is deleted.
- New code starts directly under `dev.vfyjxf.cumulus`.
- Working mod id is `cumulus`.
- Component expansion happens in canonical untransformed coordinates.
- Allowed transforms are stored as ordered descriptors on `StructureDefinition`; later plan compilation applies them in listed order when evaluating candidates.
- This slice uses `IR_SCHEMA_VERSION = 1` for in-memory definitions only. Runtime/debug asset schema compatibility is handled by a later datagen spec.
- A spatial conflict means two required points use the same relative block coordinate and same target type. Future alternatives can relax this with explicit alternative groups.
- Every commit command in this plan must use pathspecs that name only the intended files, because the repository currently has pre-existing staged template files outside this plan's task boundaries.

## File Structure

Delete:

- `src/main/java/com/example/examplemod/Config.java`
- `src/main/java/com/example/examplemod/ExampleMod.java`
- `src/main/java/com/example/examplemod/ExampleModClient.java`
- `src/main/resources/assets/examplemod/lang/en_us.json`

Modify:

- `build.gradle` - add test dependencies, JUnit Platform, GameTest server force-exit fix.
- `gradle.properties` - set `mod_id=cumulus`, `mod_name=Architect Cumulus`, `mod_group_id=dev.vfyjxf.cumulus`.
- `src/main/templates/META-INF/neoforge.mods.toml` - remove example wording and use ArchitectCumulus description.
- `README.md` - add first DSL example and test commands.

Create:

- `src/main/java/dev/vfyjxf/cumulus/Cumulus.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/StructureDefinition.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/StructurePointDefinition.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/StructurePointId.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/PointRequirement.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/RelativeBlockPos.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/Structures.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionBuilder.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/StructurePointBuilder.java`
- `src/main/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionException.java`
- `src/main/java/dev/vfyjxf/cumulus/api/component/StructureComponent.java`
- `src/main/java/dev/vfyjxf/cumulus/api/component/ComponentEmitter.java`
- `src/main/java/dev/vfyjxf/cumulus/api/component/Components.java`
- `src/main/java/dev/vfyjxf/cumulus/api/component/PointComponent.java`
- `src/main/java/dev/vfyjxf/cumulus/api/component/BoxComponent.java`
- `src/main/java/dev/vfyjxf/cumulus/api/context/ContextRuleDescriptor.java`
- `src/main/java/dev/vfyjxf/cumulus/api/diagnostic/DiagnosticMetadata.java`
- `src/main/java/dev/vfyjxf/cumulus/api/predicate/PredicateDescriptor.java`
- `src/main/java/dev/vfyjxf/cumulus/api/predicate/PredicateDescriptors.java`
- `src/main/java/dev/vfyjxf/cumulus/api/target/TargetDescriptor.java`
- `src/main/java/dev/vfyjxf/cumulus/api/target/TargetPattern.java`
- `src/main/java/dev/vfyjxf/cumulus/api/target/Targets.java`
- `src/main/java/dev/vfyjxf/cumulus/api/transform/TransformDescriptor.java`
- `src/main/java/dev/vfyjxf/cumulus/api/transform/Transforms.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/definition/StructureDefinitionCompiler.java`
- `src/main/java/dev/vfyjxf/cumulus/impl/definition/StructureDefinitionValidator.java`
- `src/main/java/dev/vfyjxf/cumulus/gametest/CumulusGameTests.java`
- `src/test/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionTest.java`
- `src/test/java/dev/vfyjxf/cumulus/api/component/BoxComponentTest.java`
- `src/test/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionBuilderTest.java`
- `src/test/java/dev/vfyjxf/cumulus/impl/definition/StructureDefinitionValidatorTest.java`
- `src/test/java/dev/vfyjxf/cumulus/neoforge/CumulusMinecraftEnvironmentTest.java`

## Task 1: Project Identity and Test Harness

**Files:**
- Modify: `gradle.properties`
- Modify: `build.gradle`
- Modify: `src/main/templates/META-INF/neoforge.mods.toml`

- [ ] **Step 1: Update project properties**

Change these values in `gradle.properties`:

```properties
mod_id=cumulus
mod_name=Architect Cumulus
mod_license=All Rights Reserved
mod_version=1.0.0
mod_group_id=dev.vfyjxf.cumulus
```

- [ ] **Step 2: Add test dependencies and JUnit Platform**

In `build.gradle`, extend the existing `dependencies` block:

```groovy
dependencies {
    testImplementation platform("org.junit:junit-bom:5.10.2")
    testImplementation "org.junit.jupiter:junit-jupiter"
    implementation "net.neoforged:testframework:${neo_version}"
}
```

`testframework` is intentionally `implementation`, not `testImplementation`, because GameTest classes are compiled from `src/main/java` and import `net.neoforged.testframework.gametest.EmptyTemplate`.

Add this near the existing `tasks.withType(JavaCompile)` block:

```groovy
tasks.named('test', Test).configure {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Make GameTest server usable from Gradle**

Inside the existing `neoForge.runs.gameTestServer` block, add:

```groovy
setForceExit false
```

Keep the existing namespace property:

```groovy
systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
```

- [ ] **Step 4: Update mod metadata text**

In `src/main/templates/META-INF/neoforge.mods.toml`, replace the example description with:

```toml
description='''
ArchitectCumulus provides reusable structure definition, validation, and checking foundations for NeoForge multiblock-style systems.
'''
```

- [ ] **Step 5: Run Gradle test task**

Run:

```bash
./gradlew test
```

Expected: build configures successfully. It may report no tests yet or pass existing discovered tests.

- [ ] **Step 6: Commit**

```bash
git add build.gradle gradle.properties src/main/templates/META-INF/neoforge.mods.toml
git commit -m "chore: configure cumulus project identity and tests" -- build.gradle gradle.properties src/main/templates/META-INF/neoforge.mods.toml
```

## Task 2: Delete Template Code and Add Minimal Mod Entrypoint

**Files:**
- Delete: `src/main/java/com/example/examplemod/Config.java`
- Delete: `src/main/java/com/example/examplemod/ExampleMod.java`
- Delete: `src/main/java/com/example/examplemod/ExampleModClient.java`
- Delete: `src/main/resources/assets/examplemod/lang/en_us.json`
- Create: `src/main/java/dev/vfyjxf/cumulus/Cumulus.java`

- [ ] **Step 1: Delete example template files**

Run:

```bash
git rm src/main/java/com/example/examplemod/Config.java \
  src/main/java/com/example/examplemod/ExampleMod.java \
  src/main/java/com/example/examplemod/ExampleModClient.java \
  src/main/resources/assets/examplemod/lang/en_us.json
```

- [ ] **Step 2: Add minimal mod class**

Create `src/main/java/dev/vfyjxf/cumulus/Cumulus.java`:

```java
package dev.vfyjxf.cumulus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Cumulus.MODID)
public final class Cumulus {
    public static final String MODID = "cumulus";

    public Cumulus(IEventBus modEventBus, ModContainer modContainer) {
    }
}
```

- [ ] **Step 3: Verify old package is gone**

Run:

```bash
rg "com\\.example|examplemod|ExampleMod" src gradle.properties build.gradle
```

Expected: no matches except historical comments if any were intentionally kept. Remove remaining template references.

- [ ] **Step 4: Compile**

Run:

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/vfyjxf/cumulus/Cumulus.java
git commit -m "chore: replace example mod with cumulus entrypoint" -- \
  src/main/java/com/example/examplemod/Config.java \
  src/main/java/com/example/examplemod/ExampleMod.java \
  src/main/java/com/example/examplemod/ExampleModClient.java \
  src/main/resources/assets/examplemod/lang/en_us.json \
  src/main/java/dev/vfyjxf/cumulus/Cumulus.java
```

## Task 3: Add Core IR Value Records

**Files:**
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/RelativeBlockPos.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/StructurePointId.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/PointRequirement.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/target/TargetDescriptor.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/target/TargetPattern.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/target/Targets.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/predicate/PredicateDescriptor.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/predicate/PredicateDescriptors.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/context/ContextRuleDescriptor.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/diagnostic/DiagnosticMetadata.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/transform/TransformDescriptor.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/transform/Transforms.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/StructurePointDefinition.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/StructureDefinition.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionException.java`
- Test: `src/test/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionTest.java`

- [ ] **Step 1: Write failing pure JUnit tests**

Create `StructureDefinitionTest` with tests for immutable copies, default schema version, and invalid point ids:

```java
package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticMetadata;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.target.TargetDescriptor;
import dev.vfyjxf.cumulus.api.transform.TransformDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructureDefinitionTest {
    @Test
    void copiesMutableInputs() {
        var predicates = new java.util.ArrayList<PredicateDescriptor>();
        predicates.add(new PredicateDescriptor(ResourceLocation.parse("cumulus:any"), Map.of()));

        var point = new StructurePointDefinition(
                new StructurePointId("controller"),
                RelativeBlockPos.of(0, 0, 0),
                new TargetDescriptor(ResourceLocation.parse("cumulus:block"), Map.of()),
                predicates,
                PointRequirement.REQUIRED,
                Set.of("controller"),
                Set.of("core"),
                DiagnosticMetadata.empty());

        predicates.clear();

        assertEquals(1, point.predicates().size());
    }

    @Test
    void definitionHasSchemaVersionOne() {
        var definition = new StructureDefinition(
                ResourceLocation.parse("cumulus:test"),
                List.of(new TransformDescriptor(ResourceLocation.parse("cumulus:identity"), Map.of())),
                List.of(),
                List.of(),
                DiagnosticMetadata.empty());

        assertEquals(1, definition.schemaVersion());
    }

    @Test
    void pointIdRejectsBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new StructurePointId(" "));
    }
}
```

- [ ] **Step 2: Run failing test**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.api.definition.StructureDefinitionTest
```

Expected: FAIL because the IR classes do not exist.

- [ ] **Step 3: Implement value records**

Implement these core shapes:

```java
public record RelativeBlockPos(int x, int y, int z) {
    public static RelativeBlockPos of(int x, int y, int z) {
        return new RelativeBlockPos(x, y, z);
    }
}
```

```java
public record StructurePointId(String value) {
    public StructurePointId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Structure point id must not be blank");
        }
    }
}
```

```java
public enum PointRequirement {
    REQUIRED,
    OPTIONAL
}
```

Descriptor records should copy maps/lists/sets with `Map.copyOf`, `List.copyOf`, and `Set.copyOf`:

```java
public record TargetDescriptor(ResourceLocation typeId, Map<String, String> properties) {
    public TargetDescriptor {
        if (typeId == null) throw new IllegalArgumentException("Target type id must not be null");
        properties = Map.copyOf(properties);
    }
}
```

Use the same defensive-copy style for `PredicateDescriptor`, `ContextRuleDescriptor`, `DiagnosticMetadata`, and `TransformDescriptor`.

Use this exact `StructurePointDefinition` record shape:

```java
public record StructurePointDefinition(
        StructurePointId id,
        RelativeBlockPos location,
        TargetDescriptor target,
        List<PredicateDescriptor> predicates,
        PointRequirement requirement,
        Set<String> captures,
        Set<String> groups,
        DiagnosticMetadata diagnostics) {
    public StructurePointDefinition {
        if (id == null) throw new IllegalArgumentException("Point id must not be null");
        if (location == null) throw new IllegalArgumentException("Point location must not be null");
        if (target == null) throw new IllegalArgumentException("Point target must not be null");
        if (requirement == null) throw new IllegalArgumentException("Point requirement must not be null");
        predicates = List.copyOf(predicates);
        captures = Set.copyOf(captures);
        groups = Set.copyOf(groups);
        if (diagnostics == null) throw new IllegalArgumentException("Point diagnostics must not be null");
    }
}
```

Use this exact `StructureDefinition` record shape:

```java
public record StructureDefinition(
        ResourceLocation id,
        int schemaVersion,
        List<TransformDescriptor> transforms,
        List<StructurePointDefinition> points,
        List<ContextRuleDescriptor> contextRules,
        DiagnosticMetadata diagnostics) {
    public static final int IR_SCHEMA_VERSION = 1;

    public StructureDefinition {
        if (id == null) throw new IllegalArgumentException("Structure id must not be null");
        transforms = List.copyOf(transforms);
        points = List.copyOf(points);
        contextRules = List.copyOf(contextRules);
        if (diagnostics == null) throw new IllegalArgumentException("Diagnostics must not be null");
    }

    public StructureDefinition(
            ResourceLocation id,
            List<TransformDescriptor> transforms,
            List<StructurePointDefinition> points,
            List<ContextRuleDescriptor> contextRules,
            DiagnosticMetadata diagnostics) {
        this(id, IR_SCHEMA_VERSION, transforms, points, contextRules, diagnostics);
    }
}
```

The five-argument constructor is the convenience constructor used by tests and builders. The six-argument canonical constructor remains available for later schema-version compatibility tests.

Add `TargetPattern` as the author-facing point target wrapper:

```java
public record TargetPattern(
        TargetDescriptor target,
        List<PredicateDescriptor> predicates,
        Set<String> captures) {
    public TargetPattern {
        predicates = List.copyOf(predicates);
        captures = Set.copyOf(captures);
    }

    public TargetPattern where(PredicateDescriptor predicate) {
        var next = new java.util.ArrayList<>(predicates);
        next.add(predicate);
        return new TargetPattern(target, next, captures);
    }

    public TargetPattern capture(String captureName) {
        var next = new java.util.LinkedHashSet<>(captures);
        next.add(captureName);
        return new TargetPattern(target, predicates, next);
    }
}
```

`Targets.block()` returns a `TargetPattern`, while `StructurePointDefinition` stores the underlying `TargetDescriptor`, predicates, and captures separately.

Also add minimal factory classes required by component tests:

```java
public final class PredicateDescriptors {
    public static PredicateDescriptor id(String id) {
        return new PredicateDescriptor(ResourceLocation.parse(id), Map.of());
    }
}
```

```java
public final class Transforms {
    public static TransformDescriptor identity() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:identity"), Map.of());
    }

    public static TransformDescriptor horizontalFacing() {
        return new TransformDescriptor(ResourceLocation.parse("cumulus:horizontal_facing"), Map.of());
    }
}
```

- [ ] **Step 4: Run test**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.api.definition.StructureDefinitionTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/vfyjxf/cumulus/api src/test/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionTest.java
git commit -m "feat: add core structure definition IR records" -- src/main/java/dev/vfyjxf/cumulus/api src/test/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionTest.java
```

## Task 4: Add Component Expansion

**Files:**
- Create: `src/main/java/dev/vfyjxf/cumulus/api/component/StructureComponent.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/component/ComponentEmitter.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/component/PointComponent.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/component/BoxComponent.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/component/Components.java`
- Test: `src/test/java/dev/vfyjxf/cumulus/api/component/BoxComponentTest.java`

- [ ] **Step 1: Write failing component tests**

Create tests that prove filled and hollow boxes expand deterministically:

```java
package dev.vfyjxf.cumulus.api.component;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.Targets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoxComponentTest {
    @Test
    void filledBoxExpandsAllPositionsInclusively() {
        var component = Components.box("casing",
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(1, 1, 1))
            .target(Targets.block())
            .where(PredicateDescriptors.id("cumulus:any"));

        var emitted = new ArrayList<StructurePointDefinition>();
        component.emit(emitted::add);

        assertEquals(8, emitted.size());
        assertEquals(RelativeBlockPos.of(0, 0, 0), emitted.get(0).location());
        assertEquals(RelativeBlockPos.of(1, 1, 1), emitted.get(7).location());
    }

    @Test
    void hollowBoxOnlyEmitsShell() {
        var component = Components.hollowBox("shell",
                RelativeBlockPos.of(0, 0, 0),
                RelativeBlockPos.of(2, 2, 2))
            .target(Targets.block());

        var emitted = new ArrayList<StructurePointDefinition>();
        component.emit(emitted::add);

        assertEquals(26, emitted.size());
    }

    @Test
    void boxRejectsInvertedBounds() {
        assertThrows(IllegalArgumentException.class, () ->
                Components.box("bad", RelativeBlockPos.of(1, 0, 0), RelativeBlockPos.of(0, 0, 0)));
    }
}
```

- [ ] **Step 2: Run failing test**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.api.component.BoxComponentTest
```

Expected: FAIL because component classes do not exist.

- [ ] **Step 3: Implement component contracts**

Implement:

```java
@FunctionalInterface
public interface ComponentEmitter {
    void point(StructurePointDefinition point);
}
```

```java
public interface StructureComponent {
    String name();
    void emit(ComponentEmitter emitter);
}
```

Component authoring objects are immutable fluent builders that also implement `StructureComponent`. Every `target(...)`, `where(...)`, `optional()`, or `group(...)` call returns a new component instance and leaves the previous instance unchanged.

Captures belong to `TargetPattern` and direct point definitions in this first slice. Multi-point components do not expose `capture(...)` yet because emitting the same capture name for many generated points would conflict with the first validator rules. Later specs can add explicit capture policies for component groups.

`PointComponent` emits exactly one point. `BoxComponent` emits deterministic x/y/z nested-loop point ids using `name_x_y_z`. `Components.box` creates a filled box; `Components.hollowBox` creates a shell-only box.

Use these method shapes:

```java
public record BoxComponent(
        String name,
        RelativeBlockPos from,
        RelativeBlockPos to,
        boolean hollow,
        TargetPattern target,
        PointRequirement requirement,
        Set<String> groups,
        DiagnosticMetadata diagnostics) implements StructureComponent {
    public BoxComponent target(TargetPattern target) { ... }
    public BoxComponent where(PredicateDescriptor predicate) { ... }
    public BoxComponent optional() { ... }
    public BoxComponent group(String group) { ... }
    @Override public void emit(ComponentEmitter emitter) { ... }
}
```

`Components.box(...)` and `Components.hollowBox(...)` default to `Targets.block()`, `PointRequirement.REQUIRED`, no groups, and `DiagnosticMetadata.empty()`. This default is why tests can call `.target(Targets.block())` explicitly or omit it for simple smoke tests.

- [ ] **Step 4: Run component tests**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.api.component.BoxComponentTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/vfyjxf/cumulus/api/component src/test/java/dev/vfyjxf/cumulus/api/component/BoxComponentTest.java
git commit -m "feat: add basic structure component expansion" -- src/main/java/dev/vfyjxf/cumulus/api/component src/test/java/dev/vfyjxf/cumulus/api/component/BoxComponentTest.java
```

## Task 5: Add Structure Definition DSL Builders

**Files:**
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/Structures.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionBuilder.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/api/definition/StructurePointBuilder.java`
- Create: `src/main/java/dev/vfyjxf/cumulus/impl/definition/StructureDefinitionCompiler.java`
- Test: `src/test/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionBuilderTest.java`

- [ ] **Step 1: Write failing DSL tests**

Create:

```java
package dev.vfyjxf.cumulus.api.definition;

import dev.vfyjxf.cumulus.api.component.Components;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.Targets;
import dev.vfyjxf.cumulus.api.transform.Transforms;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructureDefinitionBuilderTest {
    @Test
    void dslBuildsPointsComponentsCapturesAndTransforms() {
        var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
                .transforms(Transforms.identity(), Transforms.horizontalFacing())
                .point("controller", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("cumulus:controller"))
                        .capture("controller"))
                .component(Components.box("casing",
                        RelativeBlockPos.of(-1, 1, -1),
                        RelativeBlockPos.of(1, 1, 1))
                        .target(Targets.block())
                        .where(PredicateDescriptors.id("cumulus:casing")))
                .build();

        assertEquals(ResourceLocation.parse("cumulus:machine"), definition.id());
        assertEquals(2, definition.transforms().size());
        assertEquals(10, definition.points().size());
        assertEquals("controller", definition.points().get(0).captures().iterator().next());
    }
}
```

- [ ] **Step 2: Run failing test**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.api.definition.StructureDefinitionBuilderTest
```

Expected: FAIL because DSL builders do not exist.

- [ ] **Step 3: Implement builder entry points**

`Structures.define(id)` returns a mutable builder. `StructureDefinitionBuilder` collects transform descriptors, direct point builders, and components. `StructurePointBuilder` converts a `TargetPattern` into one `StructurePointDefinition`.

```java
public final class Structures {
    public static StructureDefinitionBuilder define(ResourceLocation id) {
        return new StructureDefinitionBuilder(id);
    }
}
```

- [ ] **Step 4: Implement builder compile path**

`Structures.define(id)` returns a mutable builder. `build()` delegates to `StructureDefinitionCompiler`, which:

- defaults transforms to `Transforms.identity()` when none are supplied;
- emits direct points before component points;
- copies all emitted points into `StructureDefinition`.

Validation is intentionally wired in Task 6, because `StructureDefinitionValidator` is introduced there. Task 5 should not create a temporary no-op validator.

- [ ] **Step 5: Run DSL test**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.api.definition.StructureDefinitionBuilderTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/vfyjxf/cumulus/api src/main/java/dev/vfyjxf/cumulus/impl/definition src/test/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionBuilderTest.java
git commit -m "feat: add structure definition DSL" -- src/main/java/dev/vfyjxf/cumulus/api src/main/java/dev/vfyjxf/cumulus/impl/definition src/test/java/dev/vfyjxf/cumulus/api/definition/StructureDefinitionBuilderTest.java
```

## Task 6: Add Definition Validation

**Files:**
- Create: `src/main/java/dev/vfyjxf/cumulus/impl/definition/StructureDefinitionValidator.java`
- Modify: `src/main/java/dev/vfyjxf/cumulus/impl/definition/StructureDefinitionCompiler.java`
- Test: `src/test/java/dev/vfyjxf/cumulus/impl/definition/StructureDefinitionValidatorTest.java`

- [ ] **Step 1: Write failing validation tests**

Create tests:

```java
package dev.vfyjxf.cumulus.impl.definition;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinitionException;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.target.Targets;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StructureDefinitionValidatorTest {
    @Test
    void rejectsDuplicatePointIds() {
        assertThrows(StructureDefinitionException.class, () -> Structures.define(ResourceLocation.parse("cumulus:bad"))
                .point("same", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .point("same", RelativeBlockPos.of(1, 0, 0), Targets.block())
                .build());
    }

    @Test
    void rejectsDuplicateCaptureNames() {
        assertThrows(StructureDefinitionException.class, () -> Structures.define(ResourceLocation.parse("cumulus:bad"))
                .point("a", RelativeBlockPos.of(0, 0, 0), Targets.block().capture("controller"))
                .point("b", RelativeBlockPos.of(1, 0, 0), Targets.block().capture("controller"))
                .build());
    }

    @Test
    void rejectsRequiredSpatialTargetConflict() {
        assertThrows(StructureDefinitionException.class, () -> Structures.define(ResourceLocation.parse("cumulus:bad"))
                .point("a", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .point("b", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .build());
    }
}
```

- [ ] **Step 2: Run failing validation tests**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.impl.definition.StructureDefinitionValidatorTest
```

Expected: FAIL until validator is implemented or wired into `build()`.

- [ ] **Step 3: Implement validator**

Implement `StructureDefinitionValidator.validate(StructureDefinition definition)`:

- reject null definitions;
- reject duplicate point ids;
- reject duplicate capture names across points;
- reject required point conflicts by key `(RelativeBlockPos location, ResourceLocation targetType)`;
- ignore optional points for the first conflict rule;
- throw `StructureDefinitionException` with messages containing point ids and coordinates.

- [ ] **Step 4: Run validation tests**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.impl.definition.StructureDefinitionValidatorTest
```

Expected: PASS.

- [ ] **Step 5: Run all pure JUnit tests**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.api.* --tests dev.vfyjxf.cumulus.impl.*
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/vfyjxf/cumulus/impl/definition src/test/java/dev/vfyjxf/cumulus/impl/definition
git commit -m "feat: validate structure definitions" -- src/main/java/dev/vfyjxf/cumulus/impl/definition src/test/java/dev/vfyjxf/cumulus/impl/definition
```

## Task 7: Add NeoForge JUnit Extension Coverage

**Files:**
- Test: `src/test/java/dev/vfyjxf/cumulus/neoforge/CumulusMinecraftEnvironmentTest.java`

- [ ] **Step 1: Write NeoForge JUnit test**

Create:

```java
package dev.vfyjxf.cumulus.neoforge;

import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptors;
import dev.vfyjxf.cumulus.api.target.Targets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class CumulusMinecraftEnvironmentTest {
    @Test
    void minecraftRegistryIsAvailable(MinecraftServer server) {
        assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("minecraft:stone")));
    }

    @Test
    void dslAcceptsMinecraftResourceLocations(MinecraftServer server) {
        var definition = Structures.define(ResourceLocation.parse("cumulus:stone_marker"))
                .point("stone", RelativeBlockPos.of(0, 0, 0), Targets.block()
                        .where(PredicateDescriptors.id("minecraft:stone")))
                .build();

        assertEquals(ResourceLocation.parse("cumulus:stone_marker"), definition.id());
    }
}
```

- [ ] **Step 2: Run NeoForge JUnit test**

Run:

```bash
./gradlew test --tests dev.vfyjxf.cumulus.neoforge.CumulusMinecraftEnvironmentTest
```

Expected: PASS. Logs should include an ephemeral JUnit server startup from `EphemeralTestServerProvider`.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/dev/vfyjxf/cumulus/neoforge/CumulusMinecraftEnvironmentTest.java
git commit -m "test: cover dsl in neoforge junit environment" -- src/test/java/dev/vfyjxf/cumulus/neoforge/CumulusMinecraftEnvironmentTest.java
```

## Task 8: Add GameTest Smoke Coverage

**Files:**
- Create: `src/main/java/dev/vfyjxf/cumulus/gametest/CumulusGameTests.java`

- [ ] **Step 1: Add a GameTest class**

Create:

```java
package dev.vfyjxf.cumulus.gametest;

import dev.vfyjxf.cumulus.Cumulus;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.Structures;
import dev.vfyjxf.cumulus.api.target.Targets;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.gametest.EmptyTemplate;

@GameTestHolder(Cumulus.MODID)
public final class CumulusGameTests {
    private CumulusGameTests() {
    }

    @GameTest
    @EmptyTemplate("3x3x3")
    @PrefixGameTestTemplate(false)
    public static void definitionDslBuildsInsideGameTest(GameTestHelper helper) {
        var definition = Structures.define(ResourceLocation.parse("cumulus:gametest_smoke"))
                .point("origin", RelativeBlockPos.of(0, 0, 0), Targets.block())
                .build();

        if (definition.points().size() != 1) {
            throw new GameTestAssertException("Expected one structure point");
        }

        helper.succeed();
    }
}
```

- [ ] **Step 2: Run GameTest server**

Run:

```bash
./gradlew runGameTestServer
```

Expected: `BUILD SUCCESSFUL`; no required failed GameTests. If Gradle exits non-zero because the game test server forces process exit, confirm `setForceExit false` is inside `gameTestServer`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/dev/vfyjxf/cumulus/gametest/CumulusGameTests.java build.gradle
git commit -m "test: add core dsl gametest smoke" -- src/main/java/dev/vfyjxf/cumulus/gametest/CumulusGameTests.java build.gradle
```

## Task 9: Update README With First Public API

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Replace the placeholder README text**

Update `README.md` to describe the first slice:

````markdown
# ArchitectCumulus

ArchitectCumulus is a NeoForge library for defining and validating flexible multiblock-style structures.

The current implementation provides the first foundation layer:

- immutable in-memory structure IR;
- Java definition DSL;
- point, filled box, and hollow box components;
- target, predicate, transform, context, and diagnostic descriptors;
- plain JUnit, NeoForge JUnit, and GameTest coverage.

```java
var definition = Structures.define(ResourceLocation.parse("cumulus:machine"))
    .transforms(Transforms.identity(), Transforms.horizontalFacing())
    .point("controller", RelativeBlockPos.of(0, 1, 0), Targets.block()
        .where(PredicateDescriptors.id("cumulus:controller"))
        .capture("controller"))
    .component(Components.hollowBox("casing",
        RelativeBlockPos.of(-1, 0, -1),
        RelativeBlockPos.of(1, 2, 1))
        .target(Targets.block())
        .where(PredicateDescriptors.id("cumulus:casing")))
    .build();
```

## Verification

```bash
./gradlew test
./gradlew runGameTestServer
```
````

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: describe core ir and dsl foundation" -- README.md
```

## Task 10: Final Verification

**Files:**
- Verify only.

- [ ] **Step 1: Run all unit and NeoForge JUnit tests**

Run:

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run GameTests**

Run:

```bash
./gradlew runGameTestServer
```

Expected: `BUILD SUCCESSFUL`; no required failed GameTests.

- [ ] **Step 3: Check for template leftovers**

Run:

```bash
rg "com\\.example|examplemod|ExampleMod" src build.gradle gradle.properties README.md
```

Expected: no matches.

- [ ] **Step 4: Check git status**

Run:

```bash
git status --short
```

Expected: no uncommitted changes from the implementation tasks except unrelated pre-existing files the worker did not touch.

- [ ] **Step 5: Final commit if needed**

If verification required any fixes:

Run `git status --short`, stage only the files changed by the fix, and commit with a pathspec that names only those fixed files.

Otherwise do not create an empty commit.

## Handoff Notes

- Do not implement runtime checking in this plan.
- Do not add JSON/datagen serialization in this plan.
- Keep public APIs descriptor-based; predicate execution belongs to the next predicate/target spec.
- If `ResourceLocation.parse` is unavailable in the mapped API during implementation, use the equivalent 1.21.1 factory exposed by the current mappings and keep tests proving parsing behavior.
- If `./gradlew runGameTestServer` discovers no tests, verify `neoforge.enabledGameTestNamespaces` equals `cumulus` and `CumulusGameTests` is compiled in the main source set.
