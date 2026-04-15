package dev.vfyjxf.cumulus.api.binding.plan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record BindingOp(
        String role,
        PartKind kind,
        ScanSpec scan,
        MatchSpec match
) {
    public static final Codec<BindingOp> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("role").forGetter(BindingOp::role),
            PartKind.CODEC.fieldOf("kind").forGetter(BindingOp::kind),
            ScanSpec.CODEC.fieldOf("scan").forGetter(BindingOp::scan),
            MatchSpec.CODEC.optionalFieldOf("match", MatchSpec.any()).forGetter(BindingOp::match)
    ).apply(instance, BindingOp::new));

    public BindingOp {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(scan, "scan");
        Objects.requireNonNull(match, "match");
    }

    public static BindingOp any(String role, PartKind kind, ScanSpec scan) {
        return new BindingOp(role, kind, scan, MatchSpec.any());
    }

    public static BindingOp matchId(String role, PartKind kind, ScanSpec scan, ResourceLocation id) {
        return new BindingOp(role, kind, scan, MatchSpec.id(id));
    }

    public static BindingOp matchTag(String role, PartKind kind, ScanSpec scan, ResourceLocation tag) {
        return new BindingOp(role, kind, scan, MatchSpec.tag(tag));
    }

    public static BindingOp match(String role, PartKind kind, ScanSpec scan, MatchSpec match) {
        return new BindingOp(role, kind, scan, Objects.requireNonNull(match, "match"));
    }
}
