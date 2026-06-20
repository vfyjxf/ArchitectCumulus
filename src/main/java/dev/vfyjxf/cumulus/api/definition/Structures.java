package dev.vfyjxf.cumulus.api.definition;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public final class Structures {
    private static final StructureDefinitionRegistry GLOBAL_REGISTRY = new StructureDefinitionRegistry();

    private Structures() {}

    /**
     * Shortcut for {@link ResourceLocation#parse(String)}.
     * <pre>{@code
     * Structures.define(id("cumulus:forge"))
     * }</pre>
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.parse(path);
    }

    /**
     * Start a fluent builder for a structure with the given id.
     */
    public static StructureDefinitionBuilder define(ResourceLocation id) {
        return new StructureDefinitionBuilder(id);
    }

    /**
     * Shortcut: {@code define(id("cumulus:forge"))}.
     */
    public static StructureDefinitionBuilder define(String id) {
        return new StructureDefinitionBuilder(id(id));
    }

    /**
     * Lambda-wrapped definition: the entire structure — PartPattern
     * declarations, shape matrix, transforms, context rules, check phase — is
     * written inside a single lambda block. This keeps all local variables
     * (PartPattern instances, etc.) scoped to the definition expression, so a
     * {@code static final} field can hold the full definition without leaking
     * intermediate variables:
     *
     * <pre>{@code
     * public static final StructureDefinition FORGE = Structures.define(
     *     "cumulus:forge",
     *     def -> {
     *         var casing = PartPattern.block("casing")
     *             .where(PredicateDescriptors.tag("cumulus:heat_casing"))
     *             .group("casing");
     *         var ctrl = PartPattern.block("controller")
     *             .where(PredicateDescriptors.id("cumulus:controller"))
     *             .capture("controller");
     *
     *         return def.shape(new PartPattern[][][]{ {
     *             { casing, casing, casing },
     *             { casing, ctrl,   casing },
     *             { casing, casing, casing },
     *         } })
     *         .transforms(Transforms.horizontalFacing())
     *         .build();
     *     });
     * }</pre>
     *
     * @param id       the structure id string (parsed via {@link #id})
     * @param recipe   a function that receives the builder and returns the
     *                 compiled {@link StructureDefinition} (typically by
     *                 calling {@code .build()} on it)
     */
    public static StructureDefinition define(
            String id,
            Function<StructureDefinitionBuilder, StructureDefinition> recipe) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Structure id must not be blank");
        }
        if (recipe == null) {
            throw new IllegalArgumentException("Definition recipe must not be null");
        }
        return recipe.apply(new StructureDefinitionBuilder(id(id)));
    }

    public static StructureDefinitionRegistry registry() {
        return GLOBAL_REGISTRY;
    }

    public static StructureDefinitionRegistry register(StructureDefinition definition) {
        return GLOBAL_REGISTRY.register(definition);
    }

    public static Optional<StructureDefinition> resolve(ResourceLocation id) {
        return GLOBAL_REGISTRY.resolve(id);
    }
}
