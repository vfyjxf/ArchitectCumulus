package dev.vfyjxf.cumulus.api.predicate;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class PredicateDescriptors {
    private PredicateDescriptors() {
    }

    public static PredicateDescriptor id(String id) {
        return new PredicateDescriptor(ResourceLocation.parse(id), Map.of());
    }
}
