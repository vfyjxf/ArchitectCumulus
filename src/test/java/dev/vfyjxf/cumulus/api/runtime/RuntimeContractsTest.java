package dev.vfyjxf.cumulus.api.runtime;

import dev.vfyjxf.cumulus.api.ownership.ClaimMode;
import dev.vfyjxf.cumulus.api.ownership.PartClaim;
import dev.vfyjxf.cumulus.api.ownership.StructureClaimIndex;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.request.DataRequest;
import dev.vfyjxf.cumulus.api.request.RequestLedger;
import dev.vfyjxf.cumulus.api.request.RequestPriority;
import dev.vfyjxf.cumulus.api.request.RequestReason;
import dev.vfyjxf.cumulus.api.types.ContextKey;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeContractsTest {
    @Test
    void requestLedgerDeduplicatesSectionsUntilTtlExpires() {
        var section = SectionKey.fromBlock(new BlockPos(31, 32, -1));
        assertEquals(new SectionKey(1, 2, -1), section);

        var ledger = new RequestLedger();
        var request = new DataRequest(
                        section,
                        RequestReason.bindNeedsSection,
                        RequestPriority.high,
                        20,
                        "cumulus:test/checker");

        assertEquals(1, ledger.record(0, 10, request).size());
        assertEquals(0, ledger.record(5, 10, request).size());
        assertEquals(1, ledger.record(21, 10, request).size());
    }

    @Test
    void runtimeDeltaCarriesVersionedContextForMachineLogic() {
        var key = new StructureInstanceKey(ResourceLocation.parse("cumulus:blast_furnace"), new BlockPos(10, 64, 10));
        var context = RuntimeStructureContext.builder(key).revision(7).put(ContextKey.string("facing"), "north").put(ContextKey.string("controller"), "10,64,10").build();

        var delta = new InstanceDelta(key, InstanceStatus.valid, context);

        assertEquals(7, delta.context().revision());
        assertEquals("north", delta.context().get(ContextKey.string("facing")).orElse(null));
        assertEquals(key, delta.key());
    }

    @Test
    void claimIndexAllowsSharedPartsButRejectsExclusiveConflicts() {
        var part = ResourceLocation.parse("cumulus:world_part/overworld/0_64_0");
        var first = new StructureInstanceKey(ResourceLocation.parse("cumulus:parent"), new BlockPos(0, 64, 0));
        var second = new StructureInstanceKey(ResourceLocation.parse("cumulus:child"), new BlockPos(1, 64, 0));
        var index = new StructureClaimIndex();

        assertTrue(index.claim(new PartClaim(part, first, ClaimMode.shared)).isEmpty());
        assertTrue(index.claim(new PartClaim(part, second, ClaimMode.shared)).isEmpty());

        var conflict = index.claim(new PartClaim(part, second, ClaimMode.exclusive));

        assertTrue(conflict.isPresent());
        assertEquals(first, conflict.orElseThrow().existing().owner());
    }
}
