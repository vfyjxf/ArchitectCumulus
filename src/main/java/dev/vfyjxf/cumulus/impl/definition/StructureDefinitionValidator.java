package dev.vfyjxf.cumulus.impl.definition;

import dev.vfyjxf.cumulus.api.definition.PointRequirement;
import dev.vfyjxf.cumulus.api.definition.RelativeBlockPos;
import dev.vfyjxf.cumulus.api.definition.StructureDefinition;
import dev.vfyjxf.cumulus.api.definition.StructureDefinitionException;
import dev.vfyjxf.cumulus.api.definition.StructurePointDefinition;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public final class StructureDefinitionValidator {
    private StructureDefinitionValidator() {}

    public static void validate(StructureDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Structure definition must not be null");
        }

        var pointIds = new HashMap<String, StructurePointDefinition>();
        var captures = new HashMap<String, StructurePointDefinition>();
        var requiredTargets = new HashMap<RequiredTargetKey, StructurePointDefinition>();

        for (var point : definition.points()) {
            var previousId = pointIds.putIfAbsent(point.id().value(), point);
            if (previousId != null) {
                throw new StructureDefinitionException(
                                "Duplicate structure point id '" + point.id().value() + "' at " + format(previousId.location()) + " and " + format(point.location()));
            }

            for (var capture : point.captures()) {
                var previousCapture = captures.putIfAbsent(capture, point);
                if (previousCapture != null) {
                    throw new StructureDefinitionException(
                                    "Duplicate capture '" + capture + "' on points '" + previousCapture.id().value() + "' at " + format(previousCapture.location()) + " and '" + point.id().value() + "' at " + format(point.location()));
                }
            }

            if (point.requirement() != PointRequirement.required) {
                continue;
            }

            var key = new RequiredTargetKey(point.location(), point.target().typeId());
            var previousTarget = requiredTargets.putIfAbsent(key, point);
            if (previousTarget != null) {
                throw new StructureDefinitionException(
                                "Required target conflict at " + format(point.location()) + " for target '" + point.target().typeId() + "' between points '" + previousTarget.id().value() + "' and '" + point.id().value() + "'");
            }
        }
    }

    private static String format(RelativeBlockPos location) {
        return location.x() + "," + location.y() + "," + location.z();
    }

    private record RequiredTargetKey(RelativeBlockPos location, ResourceLocation targetType) {
    }
}
