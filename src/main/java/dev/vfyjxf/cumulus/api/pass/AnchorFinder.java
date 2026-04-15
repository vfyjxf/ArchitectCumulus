package dev.vfyjxf.cumulus.api.pass;

import dev.vfyjxf.cumulus.api.context.DiscoverContext;
import dev.vfyjxf.cumulus.api.context.ProbeContext;
import net.minecraft.core.BlockPos;

import java.util.stream.Stream;

public interface AnchorFinder {
    Stream<BlockPos> discover(DiscoverContext ctx);

    ProbeResult probe(BlockPos anchorPos, ProbeContext ctx);
}
