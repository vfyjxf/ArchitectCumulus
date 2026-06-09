package dev.vfyjxf.cumulus.impl.build;

public interface BuildTarget {
    default boolean supports(BuildAction action) {
        return true;
    }

    boolean contains(BuildAction action);

    void apply(BuildAction action);
}
