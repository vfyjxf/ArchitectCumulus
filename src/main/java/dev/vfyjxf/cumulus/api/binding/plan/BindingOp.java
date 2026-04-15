package dev.vfyjxf.cumulus.api.binding.plan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 单条绑定操作：把扫描结果绑定到某个 role。
 *
 * @param role  role 名称（例如 "controller"）
 * @param kind  部件类型（方块/方块实体/实体）
 * @param scan  扫描方式（anchor/box/entity radius）
 * @param match 匹配谓词
 */
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

    /**
     * 匹配单个 id。
     */
    public static BindingOp matchId(String role, PartKind kind, ScanSpec scan, ResourceLocation id) {
        return new BindingOp(role, kind, scan, MatchSpec.id(id));
    }

    /**
     * 匹配 tag。
     */
    public static BindingOp matchTag(String role, PartKind kind, ScanSpec scan, ResourceLocation tag) {
        return new BindingOp(role, kind, scan, MatchSpec.tag(tag));
    }

    /**
     * 匹配自定义组合谓词。
     */
    public static BindingOp match(String role, PartKind kind, ScanSpec scan, MatchSpec match) {
        return new BindingOp(role, kind, scan, Objects.requireNonNull(match, "match"));
    }
}
