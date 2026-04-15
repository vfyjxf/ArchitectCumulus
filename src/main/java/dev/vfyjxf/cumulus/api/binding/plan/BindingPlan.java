package dev.vfyjxf.cumulus.api.binding.plan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        public BindingPlan build() {
            return new BindingPlan(ops);
        }
    }
}
