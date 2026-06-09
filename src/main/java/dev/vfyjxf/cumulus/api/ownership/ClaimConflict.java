package dev.vfyjxf.cumulus.api.ownership;

public record ClaimConflict(
        PartClaim existing,
        PartClaim attempted) {
    public ClaimConflict {
        if (existing == null) {
            throw new IllegalArgumentException("Existing claim must not be null");
        }
        if (attempted == null) {
            throw new IllegalArgumentException("Attempted claim must not be null");
        }
    }
}
