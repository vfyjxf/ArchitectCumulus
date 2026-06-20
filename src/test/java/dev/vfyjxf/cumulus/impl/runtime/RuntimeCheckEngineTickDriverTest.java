package dev.vfyjxf.cumulus.impl.runtime;

import dev.vfyjxf.cumulus.api.runtime.StructureInstanceKey;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCheckEngineTickDriverTest {
    @Test
    void serverPostTickDrainsEngineWithConfiguredBudget() {
        var ticks = new ArrayList<String>();
        var driver = new RuntimeCheckEngineTickDriver(
                        (tick, maxChecks) -> {
                            ticks.add(tick + ":" + maxChecks);
                            return frame(tick, maxChecks, 0);
                        },
                        () -> 1);
        driver.setMaxChecksPerTick(4);

        driver.onServerPostTick(new ServerTickEvent.Post(() -> true, null));

        assertEquals(List.of("1:4"), ticks);
        assertEquals(1, driver.currentTick());
        assertEquals(1, driver.lastFrames().size());
    }

    @Test
    void serverPostTickUsesFastRunnerByDefault() {
        var ticks = new ArrayList<String>();
        var driver = new RuntimeCheckEngineTickDriver(
                        (tick, maxChecks) -> {
                            ticks.add("diagnostic:" + tick + ":" + maxChecks);
                            return frame(tick, maxChecks, 0);
                        },
                        (tick, maxChecks) -> {
                            ticks.add("fast:" + tick + ":" + maxChecks);
                            return frame(tick, maxChecks, 0);
                        },
                        () -> 1);
        driver.setMaxChecksPerTick(3);

        driver.onServerPostTick(new ServerTickEvent.Post(() -> true, null));

        assertEquals(List.of("fast:1:3"), ticks);
        assertEquals(1, driver.lastFrames().size());
    }

    @Test
    void requestedDiagnosticTickUsesDiagnosticRunnerOnce() {
        var ticks = new ArrayList<String>();
        var driver = new RuntimeCheckEngineTickDriver(
                        (tick, maxChecks) -> {
                            ticks.add("diagnostic:" + tick + ":" + maxChecks);
                            return frame(tick, maxChecks, 0);
                        },
                        (tick, maxChecks) -> {
                            ticks.add("fast:" + tick + ":" + maxChecks);
                            return frame(tick, maxChecks, 0);
                        },
                        () -> 1);
        driver.requestDiagnosticTick();

        driver.onServerPostTick(new ServerTickEvent.Post(() -> true, null));
        driver.onServerPostTick(new ServerTickEvent.Post(() -> true, null));

        assertEquals(List.of("diagnostic:1:16", "fast:2:16"), ticks);
    }

    @Test
    void skipsWhenServerHasNoTimeAndNoWorkIsQueued() {
        var ticks = new ArrayList<String>();
        var driver = new RuntimeCheckEngineTickDriver(
                        (tick, maxChecks) -> {
                            ticks.add(tick + ":" + maxChecks);
                            return frame(tick, maxChecks, 0);
                        },
                        () -> 0);

        driver.onServerPostTick(new ServerTickEvent.Post(() -> false, null));

        assertEquals(List.of(), ticks);
        assertEquals(0, driver.currentTick());
    }

    @Test
    void continuesDrainWhenWorkRemainsQueuedEvenIfServerHasNoExtraTime() {
        var remaining = new int[]{1};
        var frameRemaining = new int[]{1};
        var driver = new RuntimeCheckEngineTickDriver(
                        (tick, maxChecks) -> {
                            var queued = frameRemaining[0];
                            frameRemaining[0] = 0;
                            remaining[0] = queued;
                            return frame(tick, maxChecks, queued);
                        },
                        () -> remaining[0]);

        driver.onServerPostTick(new ServerTickEvent.Post(() -> true, null));
        driver.onServerPostTick(new ServerTickEvent.Post(() -> false, null));

        assertEquals(2, driver.currentTick());
        assertEquals(List.of(1, 0), driver.lastFrames().stream().map(RuntimeFrameResult::remainingQueued).toList());
    }

    @Test
    void rejectsInvalidBudgets() {
        var driver = new RuntimeCheckEngineTickDriver((tick, maxChecks) -> frame(tick, maxChecks, 0), () -> 0);

        var exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> driver.setMaxChecksPerTick(0));

        assertTrue(exception.getMessage().contains("positive"));
    }

    @Test
    void registersServerPostTickListener() {
        var eventBus = new CapturingEventBus();
        var driver = new RuntimeCheckEngineTickDriver((tick, maxChecks) -> frame(tick, maxChecks, 0), () -> 1);

        driver.register(eventBus);

        assertEquals(ServerTickEvent.Post.class, eventBus.eventTypes.getFirst());
        eventBus.dispatch(new ServerTickEvent.Post(() -> true, null));
        assertEquals(1, driver.currentTick());
    }

    private static RuntimeFrameResult frame(int tick, int maxChecks, int remainingQueued) {
        return new RuntimeFrameResult(
                        tick,
                        List.<StructureInstanceKey>of(),
                        List.of(),
                        List.of(),
                        remainingQueued);
    }

    private static final class CapturingEventBus implements IEventBus {
        private final List<Class<? extends Event>> eventTypes = new ArrayList<>();
        private final List<Consumer<? extends Event>> consumers = new ArrayList<>();

        @Override
        public void register(Object target) {}

        @Override
        public <T extends Event> void addListener(Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(Class<T> eventType, Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(boolean receiveCanceled, Class<T> eventType, Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public void unregister(Object object) {}

        @Override
        public <T extends Event> T post(T event) {
            dispatch(event);
            return event;
        }

        @Override
        public <T extends Event> T post(EventPriority phase, T event) {
            dispatch(event);
            return event;
        }

        @Override
        public void start() {}

        @SuppressWarnings("unchecked")
        private <T extends Event> void dispatch(T event) {
            for (var consumer : consumers) {
                ((Consumer<T>) consumer).accept(event);
            }
        }
    }
}
