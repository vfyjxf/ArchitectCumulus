package dev.vfyjxf.cumulus.api.ownership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class StructureClaimIndex {
    private final Map<ResourceLocation, List<PartClaim>> claimsByPart = new HashMap<>();

    public Optional<ClaimConflict> claim(PartClaim claim) {
        if (claim == null) {
            throw new IllegalArgumentException("Claim must not be null");
        }

        var claims = claimsByPart.computeIfAbsent(claim.partId(), ignored -> new ArrayList<>());
        var conflict = conflictWithExisting(claims, claim);
        if (conflict.isPresent()) {
            return conflict;
        }
        claims.add(claim);
        return Optional.empty();
    }

    public Optional<ClaimConflict> conflict(PartClaim claim) {
        if (claim == null) {
            throw new IllegalArgumentException("Claim must not be null");
        }
        return conflictWithExisting(claimsByPart.getOrDefault(claim.partId(), List.of()), claim);
    }

    private static Optional<ClaimConflict> conflictWithExisting(List<PartClaim> claims, PartClaim claim) {
        for (var existing : claims) {
            if (existing.owner().equals(claim.owner())) {
                continue;
            }
            if (existing.mode() == ClaimMode.exclusive || claim.mode() == ClaimMode.exclusive) {
                return Optional.of(new ClaimConflict(existing, claim));
            }
        }
        return Optional.empty();
    }

    public List<PartClaim> claims(ResourceLocation partId) {
        if (partId == null) {
            throw new IllegalArgumentException("Part id must not be null");
        }
        return List.copyOf(claimsByPart.getOrDefault(partId, List.of()));
    }
}
