package dev.vfyjxf.cumulus.api.predicate;

@FunctionalInterface
public interface PredicateMatcher {
    boolean matches(PredicateDescriptor predicate, PredicateTarget target);
}
