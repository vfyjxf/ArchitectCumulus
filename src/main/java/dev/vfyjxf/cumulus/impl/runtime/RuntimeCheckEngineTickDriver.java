package dev.vfyjxf.cumulus.impl.runtime;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public final class RuntimeCheckEngineTickDriver {
    private static final int defaultMaxChecksPerTick = 16;
    private static final int maxRetainedFrames = 32;

    private final FrameRunner diagnosticRunner;
    private final FrameRunner fastRunner;
    private final IntSupplier pendingChecks;
    private final List<RuntimeFrameResult> lastFrames = new ArrayList<>();
    private int maxChecksPerTick = defaultMaxChecksPerTick;
    private int currentTick;
    private boolean diagnosticTickRequested;

    public RuntimeCheckEngineTickDriver(RuntimeCheckEngine engine) {
        this(engine::tick, engine::fastTick, engine::pendingChecks);
    }

    public RuntimeCheckEngineTickDriver(FrameRunner runner, IntSupplier pendingChecks) {
        this(runner, runner, pendingChecks);
    }

    public RuntimeCheckEngineTickDriver(
                    FrameRunner diagnosticRunner,
                    FrameRunner fastRunner,
                    IntSupplier pendingChecks) {
        if (diagnosticRunner == null) {
            throw new IllegalArgumentException("Runtime diagnostic frame runner must not be null");
        }
        if (fastRunner == null) {
            throw new IllegalArgumentException("Runtime fast frame runner must not be null");
        }
        if (pendingChecks == null) {
            throw new IllegalArgumentException("Pending-check supplier must not be null");
        }
        this.diagnosticRunner = diagnosticRunner;
        this.fastRunner = fastRunner;
        this.pendingChecks = pendingChecks;
    }

    public void register(IEventBus eventBus) {
        if (eventBus == null) {
            throw new IllegalArgumentException("Event bus must not be null");
        }
        eventBus.addListener(ServerTickEvent.Post.class, this::onServerPostTick);
    }

    public void onServerPostTick(ServerTickEvent.Post event) {
        if (event == null) {
            throw new IllegalArgumentException("Server tick event must not be null");
        }
        if (pendingChecks.getAsInt() == 0) {
            return;
        }
        currentTick++;
        var runner = diagnosticTickRequested ? diagnosticRunner : fastRunner;
        diagnosticTickRequested = false;
        var frame = runner.tick(currentTick, maxChecksPerTick);
        lastFrames.add(frame);
        if (lastFrames.size() > maxRetainedFrames) {
            lastFrames.removeFirst();
        }
    }

    public void requestDiagnosticTick() {
        diagnosticTickRequested = true;
    }

    public int currentTick() {
        return currentTick;
    }

    public int maxChecksPerTick() {
        return maxChecksPerTick;
    }

    public void setMaxChecksPerTick(int maxChecksPerTick) {
        if (maxChecksPerTick <= 0) {
            throw new IllegalArgumentException("Max checks per tick must be positive");
        }
        this.maxChecksPerTick = maxChecksPerTick;
    }

    public List<RuntimeFrameResult> lastFrames() {
        return List.copyOf(lastFrames);
    }

    @FunctionalInterface
    public interface FrameRunner {
        RuntimeFrameResult tick(int tick, int maxChecks);
    }
}
