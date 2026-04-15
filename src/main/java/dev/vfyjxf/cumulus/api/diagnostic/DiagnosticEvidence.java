package dev.vfyjxf.cumulus.api.diagnostic;

import dev.vfyjxf.cumulus.api.partition.SectionKey;
import net.minecraft.core.BlockPos;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record DiagnosticEvidence(
        Set<BlockPos> positions,
        Set<SectionKey> sections,
        Set<String> roles,
        Set<UUID> entities
) {
    public DiagnosticEvidence {
        positions = Set.copyOf(Objects.requireNonNull(positions, "positions"));
        sections = Set.copyOf(Objects.requireNonNull(sections, "sections"));
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        entities = Set.copyOf(Objects.requireNonNull(entities, "entities"));
    }

    public static DiagnosticEvidence empty() {
        return new DiagnosticEvidence(Set.of(), Set.of(), Set.of(), Set.of());
    }
}
