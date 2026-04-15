package dev.vfyjxf.cumulus.common.dummy;

import dev.vfyjxf.cumulus.api.partition.BlockEntityView;
import dev.vfyjxf.cumulus.api.partition.BlockStateView;
import dev.vfyjxf.cumulus.api.partition.EntityView;
import dev.vfyjxf.cumulus.api.partition.LoadState;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.partition.SectionView;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 一个内存版 {@link PartitionAccess}（测试/演示用）。
 *
 * <p>特性：
 * <ul>
 *     <li>按 {@link SectionKey} 维护 loaded/unloaded</li>
 *     <li>支持 block/blockEntity/entity 的最小读取</li>
 *     <li>支持基于 id 的 tag 映射（用于 MatchSpec.Tag）</li>
 * </ul>
 *
 * <p>注意：该实现不追求生产性能；其 {@code raw()} 字段通常直接返回 {@link ResourceLocation} 作为占位。
 */
public final class InMemoryPartitionAccess implements PartitionAccess {
    private static final BlockStateView MISSING_BLOCK = new SimpleBlockStateView(ResourceLocation.fromNamespaceAndPath("cumulus", "missing"), "missing", Set.of());

    private final Map<SectionKey, InMemorySection> sections = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> blockTagsById = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> entityTypeTagsById = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> blockEntityTypeTagsById = new HashMap<>();

    public InMemoryPartitionAccess loadSection(SectionKey key) {
        sections.computeIfAbsent(Objects.requireNonNull(key, "key"), __ -> new InMemorySection(key));
        return this;
    }

    public InMemoryPartitionAccess unloadSection(SectionKey key) {
        sections.remove(Objects.requireNonNull(key, "key"));
        return this;
    }

    public InMemoryPartitionAccess setBlock(BlockPos pos, ResourceLocation blockId) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(blockId, "blockId");
        SectionKey key = SectionKey.from(pos);
        loadSection(key);
        sections.get(key).blocks.put(pos.asLong(), new SimpleBlockStateView(blockId, blockId, blockTagsById.getOrDefault(blockId, Set.of())));
        return this;
    }

    public InMemoryPartitionAccess addEntity(BlockPos pos, ResourceLocation typeId, UUID uuid) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(uuid, "uuid");
        SectionKey key = SectionKey.from(pos);
        loadSection(key);
        sections.get(key).entities.add(new SimpleEntityView(uuid, typeId, pos.asLong(), typeId, entityTypeTagsById.getOrDefault(typeId, Set.of())));
        return this;
    }

    public InMemoryPartitionAccess setBlockEntity(BlockPos pos, ResourceLocation typeId) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(typeId, "typeId");
        SectionKey key = SectionKey.from(pos);
        loadSection(key);
        sections.get(key).blockEntities.put(pos.asLong(), new SimpleBlockEntityView(typeId, typeId, blockEntityTypeTagsById.getOrDefault(typeId, Set.of())));
        return this;
    }

    public InMemoryPartitionAccess removeBlockEntity(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        InMemorySection section = sections.get(SectionKey.from(pos));
        if (section != null) {
            section.blockEntities.remove(pos.asLong());
        }
        return this;
    }

    public InMemoryPartitionAccess addBlockToTag(ResourceLocation tagId, ResourceLocation blockId) {
        Objects.requireNonNull(tagId, "tagId");
        Objects.requireNonNull(blockId, "blockId");
        blockTagsById.computeIfAbsent(blockId, __ -> new java.util.HashSet<>()).add(tagId);
        return this;
    }

    public InMemoryPartitionAccess addEntityTypeToTag(ResourceLocation tagId, ResourceLocation entityTypeId) {
        Objects.requireNonNull(tagId, "tagId");
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        entityTypeTagsById.computeIfAbsent(entityTypeId, __ -> new java.util.HashSet<>()).add(tagId);
        return this;
    }

    public InMemoryPartitionAccess addBlockEntityTypeToTag(ResourceLocation tagId, ResourceLocation blockEntityTypeId) {
        Objects.requireNonNull(tagId, "tagId");
        Objects.requireNonNull(blockEntityTypeId, "blockEntityTypeId");
        blockEntityTypeTagsById.computeIfAbsent(blockEntityTypeId, __ -> new java.util.HashSet<>()).add(tagId);
        return this;
    }

    @Override
    public LoadState loadState(SectionKey key) {
        return sections.containsKey(key) ? LoadState.LOADED : LoadState.MISSING;
    }

    @Override
    public SectionView section(SectionKey key) {
        InMemorySection section = sections.get(key);
        if (section == null) {
            return new MissingSectionView(key);
        }
        return section.view;
    }

    @Override
    public Iterable<SectionKey> loadedSections() {
        return List.copyOf(sections.keySet());
    }

    private static final class InMemorySection {
        private final Map<Long, BlockStateView> blocks = new HashMap<>();
        private final Map<Long, BlockEntityView> blockEntities = new HashMap<>();
        private final List<EntityView> entities = new ArrayList<>();
        private final InMemorySectionView view;

        private InMemorySection(SectionKey key) {
            this.view = new InMemorySectionView(key, blocks, blockEntities, entities);
        }
    }

    private static final class InMemorySectionView implements SectionView {
        private final SectionKey key;
        private final Map<Long, BlockStateView> blocks;
        private final Map<Long, BlockEntityView> blockEntities;
        private final List<EntityView> entities;

        private InMemorySectionView(
                SectionKey key,
                Map<Long, BlockStateView> blocks,
                Map<Long, BlockEntityView> blockEntities,
                List<EntityView> entities
        ) {
            this.key = Objects.requireNonNull(key, "key");
            this.blocks = Objects.requireNonNull(blocks, "blocks");
            this.blockEntities = Objects.requireNonNull(blockEntities, "blockEntities");
            this.entities = Objects.requireNonNull(entities, "entities");
        }

        @Override
        public SectionKey key() {
            return key;
        }

        @Override
        public LoadState loadState() {
            return LoadState.LOADED;
        }

        @Override
        public BlockStateView blockState(long packedBlockPos) {
            return blocks.getOrDefault(packedBlockPos, MISSING_BLOCK);
        }

        @Override
        public Optional<BlockEntityView> blockEntity(long packedBlockPos) {
            return Optional.ofNullable(blockEntities.get(packedBlockPos));
        }

        @Override
        public Iterable<EntityView> entities() {
            return List.copyOf(entities);
        }
    }

    private record MissingSectionView(SectionKey key) implements SectionView {
        @Override
        public LoadState loadState() {
            return LoadState.MISSING;
        }

        @Override
        public BlockStateView blockState(long packedBlockPos) {
            return MISSING_BLOCK;
        }

        @Override
        public Optional<BlockEntityView> blockEntity(long packedBlockPos) {
            return Optional.empty();
        }

        @Override
        public Iterable<EntityView> entities() {
            return List.of();
        }
    }

    private record SimpleBlockStateView(ResourceLocation id, Object raw, Set<ResourceLocation> tags) implements BlockStateView {
        private SimpleBlockStateView {
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
        }

        @Override
        public boolean isInTag(ResourceLocation tagId) {
            return tags.contains(tagId);
        }
    }

    private record SimpleBlockEntityView(ResourceLocation typeId, Object raw, Set<ResourceLocation> tags) implements BlockEntityView {
        private SimpleBlockEntityView {
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
        }

        @Override
        public boolean isInTag(ResourceLocation tagId) {
            return tags.contains(tagId);
        }
    }

    private record SimpleEntityView(UUID uuid, ResourceLocation typeId, long packedBlockPos, Object raw, Set<ResourceLocation> tags) implements EntityView {
        private SimpleEntityView {
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
        }

        @Override
        public boolean isInTag(ResourceLocation tagId) {
            return tags.contains(tagId);
        }
    }
}
