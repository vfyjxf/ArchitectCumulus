package dev.vfyjxf.cumulus.api.component;

import dev.vfyjxf.cumulus.api.predicate.PredicateDescriptor;
import dev.vfyjxf.cumulus.api.target.TargetPattern;

/**
 * Shared fluent interface for all component types and {@link
 * dev.vfyjxf.cumulus.api.definition.PartPattern} that carry a point's
 * configurable properties: target, predicates, requirement, and groups.
 *
 * <p>Implemented by {@link BoxComponent}, {@link PointComponent},
 * {@link RegionComponent}, and {@link dev.vfyjxf.cumulus.api.definition.PartPattern}
 * to unify their fluent API and allow the concrete types to be package-private.
 *
 * @param <Self> the implementing type (for fluent return)
 */
public interface PointConfigured<Self extends PointConfigured<Self>> {
    Self target(TargetPattern target);

    Self where(PredicateDescriptor predicate);

    Self optional();

    Self required();

    Self group(String group);
}
