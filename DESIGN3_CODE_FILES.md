# Design3 Implementation Code Files

The `design3` implementation code is in the current working tree under these paths.

Important: many of these files are currently untracked by Git in this sandbox. They exist on disk, but the sandbox policy blocked `git add` from updating `.git/index.lock`.

## Main Implementation Root

- `src/main/java/dev/vfyjxf/cumulus/impl/design3`

This directory contains implementation code for:

- runtime plan compilation;
- check execution;
- runtime scheduling;
- runtime asset loading/exporting;
- datagen;
- automatic build;
- Minecraft adapters;
- substructure composition/lifecycle;
- editor and ghost preview rendering.

## Public API Additions

Design3 also adds API contracts under:

- `src/main/java/dev/vfyjxf/cumulus/api/component`
- `src/main/java/dev/vfyjxf/cumulus/api/datagen`
- `src/main/java/dev/vfyjxf/cumulus/api/definition`
- `src/main/java/dev/vfyjxf/cumulus/api/diagnostic`
- `src/main/java/dev/vfyjxf/cumulus/api/migration`
- `src/main/java/dev/vfyjxf/cumulus/api/ownership`
- `src/main/java/dev/vfyjxf/cumulus/api/partition`
- `src/main/java/dev/vfyjxf/cumulus/api/predicate`
- `src/main/java/dev/vfyjxf/cumulus/api/request`
- `src/main/java/dev/vfyjxf/cumulus/api/runtime`
- `src/main/java/dev/vfyjxf/cumulus/api/visualization`

## Tests

Behavior tests are under:

- `src/test/java/dev/vfyjxf/cumulus/api/design3`
- `src/test/java/dev/vfyjxf/cumulus/impl/design3`

## Fast Check Commands

Count implementation files:

```bash
find src/main/java/dev/vfyjxf/cumulus/impl/design3 -type f | wc -l
```

List implementation files:

```bash
find src/main/java/dev/vfyjxf/cumulus/impl/design3 -type f | sort
```

See untracked design3 implementation files:

```bash
git ls-files --others --exclude-standard src/main/java/dev/vfyjxf/cumulus/impl/design3 src/test/java/dev/vfyjxf/cumulus/impl/design3
```

Stage them locally, if you run this outside the sandbox:

```bash
git add README.md DESIGN3.md USAGE.md IMPLEMENTATION.md DESIGN3_CODE_FILES.md \
  docs/superpowers/specs/2026-06-08-architect-cumulus-design3.md \
  src/main/java/dev/vfyjxf/cumulus \
  src/test/java/dev/vfyjxf/cumulus
```
