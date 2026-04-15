package dev.vfyjxf.cumulus.api.binding.plan;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public sealed interface MatchSpec permits MatchSpec.Any, MatchSpec.Id, MatchSpec.Tag, MatchSpec.All, MatchSpec.AnyOf, MatchSpec.Not {
    Codec<MatchSpec> CODEC = Codec.recursive("cumulus_match_spec", self -> {
        Codec<All> allCodec = RecordCodecBuilder.create(instance -> instance.group(
                self.listOf().fieldOf("children").forGetter(All::children)
        ).apply(instance, All::new));
        Codec<AnyOf> anyOfCodec = RecordCodecBuilder.create(instance -> instance.group(
                self.listOf().fieldOf("children").forGetter(AnyOf::children)
        ).apply(instance, AnyOf::new));
        Codec<Not> notCodec = RecordCodecBuilder.create(instance -> instance.group(
                self.fieldOf("child").forGetter(Not::child)
        ).apply(instance, Not::new));

        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<MatchSpec, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> dynamic = new Dynamic<>(ops, input);
                return dynamic.get("type").asString().flatMap(type -> switch (type) {
                    case "any" -> DataResult.success(Pair.of((MatchSpec) any(), input));
                    case "id" -> Id.CODEC.decode(ops, input).map(pair -> pair.mapFirst(v -> (MatchSpec) v));
                    case "tag" -> Tag.CODEC.decode(ops, input).map(pair -> pair.mapFirst(v -> (MatchSpec) v));
                    case "all" -> allCodec.decode(ops, input).map(pair -> pair.mapFirst(v -> (MatchSpec) v));
                    case "any_of" -> anyOfCodec.decode(ops, input).map(pair -> pair.mapFirst(v -> (MatchSpec) v));
                    case "not" -> notCodec.decode(ops, input).map(pair -> pair.mapFirst(v -> (MatchSpec) v));
                    default -> DataResult.error(() -> "Unknown match type: " + type);
                });
            }

            @Override
            public <T> DataResult<T> encode(MatchSpec input, DynamicOps<T> ops, T prefix) {
                Objects.requireNonNull(input, "input");
                String type;
                DataResult<T> base;
                if (input instanceof Any) {
                    type = "any";
                    @SuppressWarnings("unchecked")
                    Map<T, T> empty = (Map<T, T>) (Map<?, ?>) Map.of();
                    base = DataResult.success(ops.createMap(empty));
                } else if (input instanceof Id id) {
                    type = "id";
                    base = Id.CODEC.encodeStart(ops, id);
                } else if (input instanceof Tag tag) {
                    type = "tag";
                    base = Tag.CODEC.encodeStart(ops, tag);
                } else if (input instanceof All all) {
                    type = "all";
                    base = allCodec.encodeStart(ops, all);
                } else if (input instanceof AnyOf anyOf) {
                    type = "any_of";
                    base = anyOfCodec.encodeStart(ops, anyOf);
                } else if (input instanceof Not not) {
                    type = "not";
                    base = notCodec.encodeStart(ops, not);
                } else {
                    return DataResult.error(() -> "Unknown match spec: " + input.getClass().getName());
                }
                return base.flatMap(encoded -> ops.mergeToMap(encoded, ops.createString("type"), ops.createString(type)));
            }
        };
    });

    static MatchSpec any() {
        return Any.INSTANCE;
    }

    static MatchSpec id(ResourceLocation id) {
        return new Id(id);
    }

    static MatchSpec tag(ResourceLocation tag) {
        return new Tag(tag);
    }

    static MatchSpec all(List<MatchSpec> children) {
        return new All(children);
    }

    static MatchSpec anyOf(List<MatchSpec> children) {
        return new AnyOf(children);
    }

    static MatchSpec not(MatchSpec child) {
        return new Not(child);
    }

    record Any() implements MatchSpec {
        private static final Any INSTANCE = new Any();
    }

    record Id(ResourceLocation id) implements MatchSpec {
        public static final Codec<Id> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Id::id)
        ).apply(instance, Id::new));

        public Id {
            Objects.requireNonNull(id, "id");
        }
    }

    record Tag(ResourceLocation tag) implements MatchSpec {
        public static final Codec<Tag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("tag").forGetter(Tag::tag)
        ).apply(instance, Tag::new));

        public Tag {
            Objects.requireNonNull(tag, "tag");
        }
    }

    record All(List<MatchSpec> children) implements MatchSpec {
        public All {
            children = List.copyOf(Objects.requireNonNull(children, "children"));
        }
    }

    record AnyOf(List<MatchSpec> children) implements MatchSpec {
        public AnyOf {
            children = List.copyOf(Objects.requireNonNull(children, "children"));
        }
    }

    record Not(MatchSpec child) implements MatchSpec {
        public Not {
            Objects.requireNonNull(child, "child");
        }
    }
}
