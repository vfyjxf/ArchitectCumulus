package dev.vfyjxf.cumulus.impl.design3.build;

public interface BuildTarget {
    default boolean supports(BuildAction action) {
        return true;
    }

    boolean contains(BuildAction action);

    void apply(BuildAction action);
}
