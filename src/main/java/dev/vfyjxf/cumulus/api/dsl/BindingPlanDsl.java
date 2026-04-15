package dev.vfyjxf.cumulus.api.dsl;

import dev.vfyjxf.cumulus.api.binding.plan.BindingOp;
import dev.vfyjxf.cumulus.api.binding.plan.BindingPlan;
import dev.vfyjxf.cumulus.api.binding.plan.MatchSpec;
import dev.vfyjxf.cumulus.api.binding.plan.PartKind;
import dev.vfyjxf.cumulus.api.binding.plan.ScanSpec;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class BindingPlanDsl {
    private final BindingPlan.Builder builder = BindingPlan.builder();

    public RoleStage role(String role) {
        return new RoleStage(this, Objects.requireNonNull(role, "role"));
    }

    public BindingPlan build() {
        return builder.build();
    }

    private BindingPlanDsl add(BindingOp op) {
        builder.add(op);
        return this;
    }

    public static final class RoleStage {
        private final BindingPlanDsl parent;
        private final String role;

        private RoleStage(BindingPlanDsl parent, String role) {
            this.parent = parent;
            this.role = role;
        }

        public BindingPlanDsl fromAnchorBlock() {
            return parent.add(BindingOp.any(role, PartKind.BLOCK, ScanSpec.Anchor.atAnchor()));
        }

        public BindingPlanDsl fromAnchorBlockEntity() {
            return parent.add(BindingOp.any(role, PartKind.BLOCK_ENTITY, ScanSpec.Anchor.atAnchor()));
        }

        public BlockBoxStage scanBoxBlocks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            return new BlockBoxStage(parent, role, new ScanSpec.Box(minX, minY, minZ, maxX, maxY, maxZ));
        }

        public BlockEntityBoxStage scanBoxBlockEntities(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            return new BlockEntityBoxStage(parent, role, new ScanSpec.Box(minX, minY, minZ, maxX, maxY, maxZ));
        }

        public EntityRadiusStage scanEntities(int radius) {
            return new EntityRadiusStage(parent, role, new ScanSpec.EntityRadius(radius));
        }

        public EntityBoxStage scanEntitiesBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            return new EntityBoxStage(parent, role, new ScanSpec.Box(minX, minY, minZ, maxX, maxY, maxZ));
        }
    }

    public static final class BlockBoxStage {
        private final BindingPlanDsl parent;
        private final String role;
        private final ScanSpec.Box scan;

        private BlockBoxStage(BindingPlanDsl parent, String role, ScanSpec.Box scan) {
            this.parent = parent;
            this.role = role;
            this.scan = scan;
        }

        public BindingPlanDsl any() {
            return parent.add(BindingOp.any(role, PartKind.BLOCK, scan));
        }

        public BindingPlanDsl matchBlockId(ResourceLocation id) {
            return parent.add(BindingOp.matchId(role, PartKind.BLOCK, scan, id));
        }

        public BindingPlanDsl matchBlockTag(ResourceLocation tag) {
            return parent.add(BindingOp.matchTag(role, PartKind.BLOCK, scan, tag));
        }

        public BindingPlanDsl match(MatchSpec match) {
            return parent.add(BindingOp.match(role, PartKind.BLOCK, scan, match));
        }
    }

    public static final class BlockEntityBoxStage {
        private final BindingPlanDsl parent;
        private final String role;
        private final ScanSpec.Box scan;

        private BlockEntityBoxStage(BindingPlanDsl parent, String role, ScanSpec.Box scan) {
            this.parent = parent;
            this.role = role;
            this.scan = scan;
        }

        public BindingPlanDsl any() {
            return parent.add(BindingOp.any(role, PartKind.BLOCK_ENTITY, scan));
        }

        public BindingPlanDsl matchBlockEntityType(ResourceLocation id) {
            return parent.add(BindingOp.matchId(role, PartKind.BLOCK_ENTITY, scan, id));
        }

        public BindingPlanDsl match(MatchSpec match) {
            return parent.add(BindingOp.match(role, PartKind.BLOCK_ENTITY, scan, match));
        }
    }

    public static final class EntityRadiusStage {
        private final BindingPlanDsl parent;
        private final String role;
        private final ScanSpec.EntityRadius scan;

        private EntityRadiusStage(BindingPlanDsl parent, String role, ScanSpec.EntityRadius scan) {
            this.parent = parent;
            this.role = role;
            this.scan = scan;
        }

        public BindingPlanDsl any() {
            return parent.add(BindingOp.any(role, PartKind.ENTITY, scan));
        }

        public BindingPlanDsl matchEntityType(ResourceLocation id) {
            return parent.add(BindingOp.matchId(role, PartKind.ENTITY, scan, id));
        }

        public BindingPlanDsl matchEntityTypeTag(ResourceLocation tag) {
            return parent.add(BindingOp.matchTag(role, PartKind.ENTITY, scan, tag));
        }

        public BindingPlanDsl match(MatchSpec match) {
            return parent.add(BindingOp.match(role, PartKind.ENTITY, scan, match));
        }
    }

    public static final class EntityBoxStage {
        private final BindingPlanDsl parent;
        private final String role;
        private final ScanSpec.Box scan;

        private EntityBoxStage(BindingPlanDsl parent, String role, ScanSpec.Box scan) {
            this.parent = parent;
            this.role = role;
            this.scan = scan;
        }

        public BindingPlanDsl any() {
            return parent.add(BindingOp.any(role, PartKind.ENTITY, scan));
        }

        public BindingPlanDsl matchEntityType(ResourceLocation id) {
            return parent.add(BindingOp.matchId(role, PartKind.ENTITY, scan, id));
        }

        public BindingPlanDsl matchEntityTypeTag(ResourceLocation tag) {
            return parent.add(BindingOp.matchTag(role, PartKind.ENTITY, scan, tag));
        }

        public BindingPlanDsl match(MatchSpec match) {
            return parent.add(BindingOp.match(role, PartKind.ENTITY, scan, match));
        }
    }
}
