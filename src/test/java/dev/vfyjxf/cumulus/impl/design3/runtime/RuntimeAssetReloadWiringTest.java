package dev.vfyjxf.cumulus.impl.design3.runtime;

import dev.vfyjxf.cumulus.impl.design3.codec.RuntimeAssetReloadListener;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeAssetReloadWiringTest {
    @Test
    void registersReloadListenerOnCommonEventBus() {
        var eventBus = new CapturingEventBus();
        var listener = new RuntimeAssetReloadListener();

        new RuntimeAssetReloadWiring(listener).register(eventBus);

        assertEquals(AddReloadListenerEvent.class, eventBus.eventTypes.getFirst());
        var event = new AddReloadListenerEvent(null, null);
        eventBus.dispatch(event);

        assertEquals(1, event.getListeners().size());
        event.getListeners().getFirst().reload(
                        CompletableFuture::completedFuture,
                        manager(),
                        net.minecraft.util.profiling.InactiveProfiler.INSTANCE,
                        net.minecraft.util.profiling.InactiveProfiler.INSTANCE,
                        Runnable::run,
                        Runnable::run)
                .join();

        assertEquals(ResourceLocation.parse("cumulus:registered_asset"),
                listener.lastResult().orElseThrow().assets().getFirst().id());
    }

    private static ResourceManager manager() {
        return new FakeResourceManager(Map.of(
                ResourceLocation.parse("cumulus:cumulus/structures/registered_asset.json"),
                new Resource(
                        null,
                        () -> new ByteArrayInputStream(("""
                                {
                                  "id": "cumulus:registered_asset",
                                  "schema_version": 1,
                                  "definition": "cumulus:registered_asset",
                                  "debug_asset": false,
                                  "runtime_plan": {
                                    "definition": "cumulus:registered_asset",
                                    "entries": [
                                      {
                                        "id": "origin",
                                        "target": "cumulus:block",
                                        "min": "0,0,0",
                                        "max": "0,0,0"
                                      }
                                    ]
                                  }
                                }
                                """).getBytes(StandardCharsets.UTF_8)))));
    }

    private static final class CapturingEventBus implements IEventBus {
        private final List<Class<? extends Event>> eventTypes = new ArrayList<>();
        private final List<Consumer<? extends Event>> consumers = new ArrayList<>();

        @Override
        public void register(Object target) {
        }

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
        public <T extends Event> void addListener(
                EventPriority priority,
                boolean receiveCanceled,
                Class<T> eventType,
                Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> consumer) {
            consumers.add(consumer);
        }

        @Override
        public <T extends Event> void addListener(
                boolean receiveCanceled,
                Class<T> eventType,
                Consumer<T> consumer) {
            eventTypes.add(eventType);
            consumers.add(consumer);
        }

        @Override
        public void unregister(Object object) {
        }

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
        public void start() {
        }

        @SuppressWarnings("unchecked")
        private <T extends Event> void dispatch(T event) {
            for (var consumer : consumers) {
                ((Consumer<T>) consumer).accept(event);
            }
        }
    }

    private record FakeResourceManager(Map<ResourceLocation, Resource> resources) implements ResourceManager {
        @Override
        public Set<String> getNamespaces() {
            return Set.of("cumulus");
        }

        @Override
        public Optional<Resource> getResource(ResourceLocation location) {
            return Optional.ofNullable(resources.get(location));
        }

        @Override
        public List<Resource> getResourceStack(ResourceLocation location) {
            return getResource(location).stream().toList();
        }

        @Override
        public Map<ResourceLocation, Resource> listResources(
                String path,
                Predicate<ResourceLocation> filter) {
            return resources.entrySet().stream()
                    .filter(entry -> entry.getKey().getPath().startsWith(path))
                    .filter(entry -> filter.test(entry.getKey()))
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue));
        }

        @Override
        public Map<ResourceLocation, List<Resource>> listResourceStacks(
                String path,
                Predicate<ResourceLocation> filter) {
            return listResources(path, filter).entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> List.of(entry.getValue())));
        }

        @Override
        public Stream<PackResources> listPacks() {
            return Stream.of();
        }
    }
}
