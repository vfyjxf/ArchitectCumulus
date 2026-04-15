package dev.vfyjxf.cumulus.common.driver;

import dev.vfyjxf.cumulus.api.driver.DriverContext;
import dev.vfyjxf.cumulus.api.driver.DriverOutput;
import dev.vfyjxf.cumulus.api.driver.DriverStrategy;
import dev.vfyjxf.cumulus.api.engine.ChangeCause;
import dev.vfyjxf.cumulus.api.engine.InstanceView;
import dev.vfyjxf.cumulus.api.partition.EntityView;
import dev.vfyjxf.cumulus.api.partition.LoadState;
import dev.vfyjxf.cumulus.api.partition.SectionKey;
import dev.vfyjxf.cumulus.api.partition.SectionView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class InterestEntityPollDriver implements DriverStrategy {
    private final int sectionsPerTick;

    private final Map<SectionKey, Long> lastHashBySection = new HashMap<>();
    private List<SectionKey> cursorSnapshot = List.of();
    private int cursor = 0;

    public InterestEntityPollDriver(int sectionsPerTick) {
        if (sectionsPerTick <= 0) {
            throw new IllegalArgumentException("sectionsPerTick must be > 0");
        }
        this.sectionsPerTick = sectionsPerTick;
    }

    public static InterestEntityPollDriver defaultInterestOnly() {
        return new InterestEntityPollDriver(32);
    }

    @Override
    public DriverOutput run(DriverContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        Set<SectionKey> interested = new HashSet<>();
        for (InstanceView view : ctx.instances().views()) {
            interested.addAll(view.interest().sections());
        }

        lastHashBySection.keySet().retainAll(interested);
        if (cursorSnapshot.isEmpty() || cursorSnapshot.size() != interested.size()) {
            cursorSnapshot = new ArrayList<>(interested);
            cursor = Math.min(cursor, Math.max(0, cursorSnapshot.size() - 1));
        }

        if (cursorSnapshot.isEmpty()) {
            return DriverOutput.empty(ChangeCause.SCAN);
        }

        Set<SectionKey> dirty = new HashSet<>();
        for (int i = 0; i < sectionsPerTick; i++) {
            if (!ctx.budget().tryConsume(1)) {
                break;
            }
            if (cursorSnapshot.isEmpty()) {
                break;
            }
            if (cursor >= cursorSnapshot.size()) {
                cursor = 0;
            }
            SectionKey sectionKey = cursorSnapshot.get(cursor++);
            if (ctx.world().loadState(sectionKey) != LoadState.LOADED) {
                continue;
            }
            SectionView section = ctx.world().section(sectionKey);
            long hash = hashEntities(ctx, section.entities());
            Long prev = lastHashBySection.put(sectionKey, hash);
            if (prev == null || prev.longValue() != hash) {
                dirty.add(sectionKey);
            }
        }

        return new DriverOutput(dirty, Set.of(), List.of(), ChangeCause.SCAN);
    }

    private static long hashEntities(DriverContext ctx, Iterable<EntityView> entities) {
        long hash = 1469598103934665603L;
        for (EntityView entity : entities) {
            if (!ctx.budget().tryConsume(1)) {
                break;
            }
            hash ^= entity.uuid().getMostSignificantBits();
            hash *= 1099511628211L;
            hash ^= entity.uuid().getLeastSignificantBits();
            hash *= 1099511628211L;
            hash ^= entity.typeId().hashCode();
            hash *= 1099511628211L;
            hash ^= entity.packedBlockPos();
            hash *= 1099511628211L;
        }
        return hash;
    }
}

