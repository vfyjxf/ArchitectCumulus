package dev.vfyjxf.cumulus.api.partition;

public interface PartitionAccess {
    LoadState loadState(SectionKey key);

    SectionView section(SectionKey key);

    default Iterable<SectionKey> loadedSections() {
        return java.util.List.of();
    }
}
