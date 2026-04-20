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
