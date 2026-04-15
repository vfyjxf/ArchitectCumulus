package dev.vfyjxf.cumulus.api.binding.plan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 声明式绑定计划：一组 {@link BindingOp}。
 *
 * <p>该计划用于描述 binder “去哪里扫描 / 扫描什么类型 / 如何匹配 / 绑定到哪个 role”。
 * common 中的 {@code PlanBinder} 可以直接执行该计划。
 *
 * <p>该类型提供 {@link #CODEC}，用于数据驱动（JSON）描述。
 */
public record BindingPlan(List<BindingOp> ops) {
    public static final Codec<BindingPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BindingOp.CODEC.listOf().fieldOf("ops").forGetter(BindingPlan::ops)
    ).apply(instance, BindingPlan::new));

    public BindingPlan {
        ops = List.copyOf(Objects.requireNonNull(ops, "ops"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<BindingOp> ops = new ArrayList<>();

        private Builder() {
        }

        public Builder add(BindingOp op) {
            ops.add(Objects.requireNonNull(op, "op"));
            return this;
        }

        /**
         * 构建计划。
         */
        public BindingPlan build() {
            return new BindingPlan(ops);
        }
    }
}
