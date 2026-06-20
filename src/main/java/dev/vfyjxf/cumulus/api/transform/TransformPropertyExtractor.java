package dev.vfyjxf.cumulus.api.transform;

import dev.vfyjxf.cumulus.api.transform.types.HorizontalFacingTransformType;
import dev.vfyjxf.cumulus.api.transform.types.IdentityTransformType;
import dev.vfyjxf.cumulus.api.transform.types.UnknownTransformType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Internal helper: extract/build typed {@link TransformProperties}.
 * Mirrors {@link dev.vfyjxf.cumulus.api.target.PropertyExtractor}.
 */
public final class TransformPropertyExtractor {

    private TransformPropertyExtractor() {}

    public static Map<String, String> extract(TransformProperties props) {
        var map = new LinkedHashMap<String, String>();
        if (props instanceof UnknownTransformType.Props p) {
            map.putAll(p.raw());
        }
        // Identity, HorizontalFacing carry no properties
        return Map.copyOf(map);
    }

    public static TransformProperties buildProps(TransformType type, Map<String, String> properties) {
        if (type instanceof IdentityTransformType) {
            return new IdentityTransformType.Props();
        }
        if (type instanceof HorizontalFacingTransformType) {
            return new HorizontalFacingTransformType.Props();
        }
        if (type instanceof UnknownTransformType) {
            return new UnknownTransformType.Props(properties);
        }
        return new IdentityTransformType.Props();
    }
}
