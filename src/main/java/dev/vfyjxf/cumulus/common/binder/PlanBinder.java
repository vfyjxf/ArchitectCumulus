package dev.vfyjxf.cumulus.common.binder;

import dev.vfyjxf.cumulus.api.binding.Bindings;
import dev.vfyjxf.cumulus.api.binding.PartRef;
import dev.vfyjxf.cumulus.api.binding.BlockEntityPartRef;
import dev.vfyjxf.cumulus.api.binding.BlockPartRef;
import dev.vfyjxf.cumulus.api.binding.EntityPartRef;
import dev.vfyjxf.cumulus.api.binding.plan.BindingOp;
import dev.vfyjxf.cumulus.api.binding.plan.BindingPlan;
import dev.vfyjxf.cumulus.api.binding.plan.MatchSpec;
import dev.vfyjxf.cumulus.api.binding.plan.PartKind;
import dev.vfyjxf.cumulus.api.binding.plan.ScanSpec;
import dev.vfyjxf.cumulus.api.context.BindContext;
import dev.vfyjxf.cumulus.api.diagnostic.Diagnostic;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEvidence;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import dev.vfyjxf.cumulus.api.interest.InterestUpdate;
import dev.vfyjxf.cumulus.api.partition.BlockEntityView;
import dev.vfyjxf.cumulus.api.partition.BlockStateView;
import dev.vfyjxf.cumulus.api.partition.EntityView;
import dev.vfyjxf.cumulus.api.partition.LoadState;
import dev.vfyjxf.cumulus.api.partition.PartitionAccess;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.partition.SectionView;
import dev.vfyjxf.cumulus.api.pass.BindReport;
import dev.vfyjxf.cumulus.api.pass.Binder;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PlanBinder implements Binder {
    private static final ResourceLocation REASON_BIND_SECTION_MISSING = ResourceLocation.fromNamespaceAndPath("cumulus", "bind_section_missing");
    private static final ResourceLocation DIAG_UNSUPPORTED = ResourceLocation.fromNamespaceAndPath("cumulus", "plan_binder_unsupported");

    private final BindingPlan plan;
    private final Duration requestTtl;
    private final String consumerId;

    public PlanBinder(BindingPlan plan) {
        this(plan, Duration.ofSeconds(10), "plan_binder");
    }

    public PlanBinder(BindingPlan plan, Duration requestTtl, String consumerId) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.requestTtl = Objects.requireNonNull(requestTtl, "requestTtl");
        this.consumerId = Objects.requireNonNull(consumerId, "consumerId");
    }

    public static PlanBinder of(BindingPlan plan) {
        return new PlanBinder(plan);
    }

    @Override
    public BindReport bind(BindContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        PartitionAccess world = ctx.world();
        BlockPos anchorPos = ctx.anchorPos();

        Set<SectionKey> footprint = new HashSet<>();
        List<DataRequest> requests = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (BindingOp op : plan.ops()) {
            footprint.addAll(footprint(anchorPos, op.scan()));
        }

        for (SectionKey section : footprint) {
            if (world.loadState(section) == LoadState.MISSING) {
                requests.add(DataRequest.forInstance(section, REASON_BIND_SECTION_MISSING, RequestPriority.NORMAL, requestTtl, ctx.key(), consumerId));
            }
        }

        InterestUpdate interest = new InterestUpdate(footprint, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Optional.empty());

        if (!requests.isEmpty()) {
            return BindReport.incomplete(requests, diagnostics, interest);
        }

        Map<String, List<PartRef>> byRole = new HashMap<>();
        for (BindingOp op : plan.ops()) {
            List<PartRef> out = scan(ctx, anchorPos, op, diagnostics);
            if (!out.isEmpty()) {
                byRole.computeIfAbsent(op.role(), __ -> new ArrayList<>()).addAll(out);
            }
        }

        return BindReport.ok(new Bindings(byRole), interest);
    }

    private static Set<SectionKey> footprint(BlockPos anchorPos, ScanSpec scan) {
        Objects.requireNonNull(anchorPos, "anchorPos");
        Objects.requireNonNull(scan, "scan");
        if (scan instanceof ScanSpec.Anchor a) {
            BlockPos pos = anchorPos.offset(a.offsetX(), a.offsetY(), a.offsetZ());
            return Set.of(SectionKey.from(pos));
        }
        if (scan instanceof ScanSpec.Box b) {
            BlockPos min = anchorPos.offset(b.minX(), b.minY(), b.minZ());
            BlockPos max = anchorPos.offset(b.maxX(), b.maxY(), b.maxZ());
            return sectionRange(min, max);
        }
        if (scan instanceof ScanSpec.EntityRadius r) {
            BlockPos min = anchorPos.offset(-r.radius(), -r.radius(), -r.radius());
            BlockPos max = anchorPos.offset(r.radius(), r.radius(), r.radius());
            return sectionRange(min, max);
        }
        return Set.of(SectionKey.from(anchorPos));
    }

    private static Set<SectionKey> sectionRange(BlockPos min, BlockPos max) {
        int minChunkX = SectionPos.blockToSectionCoord(Math.min(min.getX(), max.getX()));
        int maxChunkX = SectionPos.blockToSectionCoord(Math.max(min.getX(), max.getX()));
        int minChunkZ = SectionPos.blockToSectionCoord(Math.min(min.getZ(), max.getZ()));
        int maxChunkZ = SectionPos.blockToSectionCoord(Math.max(min.getZ(), max.getZ()));
        int minSectionY = SectionPos.blockToSectionCoord(Math.min(min.getY(), max.getY()));
        int maxSectionY = SectionPos.blockToSectionCoord(Math.max(min.getY(), max.getY()));

        Set<SectionKey> out = new HashSet<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                for (int sy = minSectionY; sy <= maxSectionY; sy++) {
                    out.add(new SectionKey(cx, sy, cz));
                }
            }
        }
        return out;
    }

    private List<PartRef> scan(BindContext ctx, BlockPos anchorPos, BindingOp op, List<Diagnostic> diagnostics) {
        if (op.scan() instanceof ScanSpec.Anchor a) {
            BlockPos pos = anchorPos.offset(a.offsetX(), a.offsetY(), a.offsetZ());
            return scanAt(ctx.world(), pos, op, diagnostics);
        }
        if (op.scan() instanceof ScanSpec.Box b) {
            return scanBox(ctx.world(), anchorPos, b, op, diagnostics);
        }
        if (op.scan() instanceof ScanSpec.EntityRadius r) {
            if (op.kind() == PartKind.ENTITY) {
                return scanEntitiesRadius(ctx.world(), anchorPos, r.radius(), op.match());
            }
            diagnostics.add(Diagnostic.of(DIAG_UNSUPPORTED, Severity.WARN, "EntityRadius scan only supports ENTITY kind", DiagnosticEvidence.empty()));
            return List.of();
        }
        diagnostics.add(Diagnostic.of(DIAG_UNSUPPORTED, Severity.WARN, "Unknown scan spec: " + op.scan().getClass().getName(), DiagnosticEvidence.empty()));
        return List.of();
    }

    private List<PartRef> scanAt(PartitionAccess world, BlockPos pos, BindingOp op, List<Diagnostic> diagnostics) {
        SectionView section = world.section(SectionKey.from(pos));
        long packed = pos.asLong();
        if (op.kind() == PartKind.BLOCK) {
            BlockStateView state = section.blockState(packed);
            if (matchesBlock(op.match(), state)) {
                return List.of(new BlockPartRef(pos));
            }
            return List.of();
        }
        if (op.kind() == PartKind.BLOCK_ENTITY) {
            Optional<BlockEntityView> be = section.blockEntity(packed);
            if (be.isPresent() && matchesBlockEntity(op.match(), be.get())) {
                return List.of(new BlockEntityPartRef(pos));
            }
            return List.of();
        }
        if (op.kind() == PartKind.ENTITY) {
            // Entities are not indexed by exact blockpos at this layer; use a radius scan for entities.
            diagnostics.add(Diagnostic.of(DIAG_UNSUPPORTED, Severity.WARN, "Anchor scan does not support ENTITY kind; use scanEntities(radius)", DiagnosticEvidence.empty()));
            return List.of();
        }
        return List.of();
    }

    private List<PartRef> scanBox(PartitionAccess world, BlockPos anchorPos, ScanSpec.Box box, BindingOp op, List<Diagnostic> diagnostics) {
        if (op.kind() == PartKind.ENTITY) {
            return scanEntitiesBox(world, anchorPos, box, op.match());
        }
        List<PartRef> out = new ArrayList<>();
        int minX = Math.min(box.minX(), box.maxX());
        int minY = Math.min(box.minY(), box.maxY());
        int minZ = Math.min(box.minZ(), box.maxZ());
        int maxX = Math.max(box.minX(), box.maxX());
        int maxY = Math.max(box.minY(), box.maxY());
        int maxZ = Math.max(box.minZ(), box.maxZ());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = minX; dx <= maxX; dx++) {
            for (int dy = minY; dy <= maxY; dy++) {
                for (int dz = minZ; dz <= maxZ; dz++) {
                    cursor.set(anchorPos.getX() + dx, anchorPos.getY() + dy, anchorPos.getZ() + dz);
                    SectionView section = world.section(SectionKey.from(cursor));
                    long packed = cursor.asLong();
                    if (op.kind() == PartKind.BLOCK) {
                        BlockStateView state = section.blockState(packed);
                        if (matchesBlock(op.match(), state)) {
                            out.add(new BlockPartRef(cursor.immutable()));
                        }
                    } else if (op.kind() == PartKind.BLOCK_ENTITY) {
                        Optional<BlockEntityView> be = section.blockEntity(packed);
                        if (be.isPresent() && matchesBlockEntity(op.match(), be.get())) {
                            out.add(new BlockEntityPartRef(cursor.immutable()));
                        }
                    } else {
                        diagnostics.add(Diagnostic.of(DIAG_UNSUPPORTED, Severity.WARN, "Unknown PartKind: " + op.kind(), DiagnosticEvidence.empty()));
                        return List.of();
                    }
                }
            }
        }
        return out;
    }

    private List<PartRef> scanEntitiesBox(PartitionAccess world, BlockPos anchorPos, ScanSpec.Box box, MatchSpec match) {
        int minX = anchorPos.getX() + Math.min(box.minX(), box.maxX());
        int minY = anchorPos.getY() + Math.min(box.minY(), box.maxY());
        int minZ = anchorPos.getZ() + Math.min(box.minZ(), box.maxZ());
        int maxX = anchorPos.getX() + Math.max(box.minX(), box.maxX());
        int maxY = anchorPos.getY() + Math.max(box.minY(), box.maxY());
        int maxZ = anchorPos.getZ() + Math.max(box.minZ(), box.maxZ());
        Set<SectionKey> sections = sectionRange(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        List<PartRef> out = new ArrayList<>();
        for (SectionKey sectionKey : sections) {
            SectionView view = world.section(sectionKey);
            for (EntityView entity : view.entities()) {
                long packed = entity.packedBlockPos();
                int ex = BlockPos.getX(packed);
                int ey = BlockPos.getY(packed);
                int ez = BlockPos.getZ(packed);
                if (ex < minX || ex > maxX || ey < minY || ey > maxY || ez < minZ || ez > maxZ) {
                    continue;
                }
                if (matchesEntity(match, entity)) {
                    out.add(new EntityPartRef(entity.uuid()));
                }
            }
        }
        return out;
    }

    private List<PartRef> scanEntitiesRadius(PartitionAccess world, BlockPos anchorPos, int radius, MatchSpec match) {
        int minX = anchorPos.getX() - radius;
        int minY = anchorPos.getY() - radius;
        int minZ = anchorPos.getZ() - radius;
        int maxX = anchorPos.getX() + radius;
        int maxY = anchorPos.getY() + radius;
        int maxZ = anchorPos.getZ() + radius;
        long r2 = (long) radius * (long) radius;

        Set<SectionKey> sections = sectionRange(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        List<PartRef> out = new ArrayList<>();
        for (SectionKey sectionKey : sections) {
            SectionView view = world.section(sectionKey);
            for (EntityView entity : view.entities()) {
                long packed = entity.packedBlockPos();
                int ex = BlockPos.getX(packed);
                int ey = BlockPos.getY(packed);
                int ez = BlockPos.getZ(packed);
                if (ex < minX || ex > maxX || ey < minY || ey > maxY || ez < minZ || ez > maxZ) {
                    continue;
                }
                long dx = (long) ex - anchorPos.getX();
                long dy = (long) ey - anchorPos.getY();
                long dz = (long) ez - anchorPos.getZ();
                if (dx * dx + dy * dy + dz * dz > r2) {
                    continue;
                }
                if (matchesEntity(match, entity)) {
                    out.add(new EntityPartRef(entity.uuid()));
                }
            }
        }
        return out;
    }

    private static boolean matchesBlock(MatchSpec match, BlockStateView state) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(state, "state");
        if (match instanceof MatchSpec.Any) {
            return true;
        }
        if (match instanceof MatchSpec.Id id) {
            return id.id().equals(state.id());
        }
        if (match instanceof MatchSpec.Tag tag) {
            return state.isInTag(tag.tag());
        }
        if (match instanceof MatchSpec.All all) {
            for (MatchSpec child : all.children()) {
                if (!matchesBlock(child, state)) {
                    return false;
                }
            }
            return true;
        }
        if (match instanceof MatchSpec.AnyOf anyOf) {
            for (MatchSpec child : anyOf.children()) {
                if (matchesBlock(child, state)) {
                    return true;
                }
            }
            return false;
        }
        if (match instanceof MatchSpec.Not not) {
            return !matchesBlock(not.child(), state);
        }
        return false;
    }

    private static boolean matchesBlockEntity(MatchSpec match, BlockEntityView be) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(be, "be");
        if (match instanceof MatchSpec.Any) {
            return true;
        }
        if (match instanceof MatchSpec.Id id) {
            return id.id().equals(be.typeId());
        }
        if (match instanceof MatchSpec.Tag tag) {
            return be.isInTag(tag.tag());
        }
        if (match instanceof MatchSpec.All all) {
            for (MatchSpec child : all.children()) {
                if (!matchesBlockEntity(child, be)) {
                    return false;
                }
            }
            return true;
        }
        if (match instanceof MatchSpec.AnyOf anyOf) {
            for (MatchSpec child : anyOf.children()) {
                if (matchesBlockEntity(child, be)) {
                    return true;
                }
            }
            return false;
        }
        if (match instanceof MatchSpec.Not not) {
            return !matchesBlockEntity(not.child(), be);
        }
        return false;
    }

    private static boolean matchesEntity(MatchSpec match, EntityView entity) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(entity, "entity");
        if (match instanceof MatchSpec.Any) {
            return true;
        }
        if (match instanceof MatchSpec.Id id) {
            return id.id().equals(entity.typeId());
        }
        if (match instanceof MatchSpec.Tag tag) {
            return entity.isInTag(tag.tag());
        }
        if (match instanceof MatchSpec.All all) {
            for (MatchSpec child : all.children()) {
                if (!matchesEntity(child, entity)) {
                    return false;
                }
            }
            return true;
        }
        if (match instanceof MatchSpec.AnyOf anyOf) {
            for (MatchSpec child : anyOf.children()) {
                if (matchesEntity(child, entity)) {
                    return true;
                }
            }
            return false;
        }
        if (match instanceof MatchSpec.Not not) {
            return !matchesEntity(not.child(), entity);
        }
        return false;
    }
}
