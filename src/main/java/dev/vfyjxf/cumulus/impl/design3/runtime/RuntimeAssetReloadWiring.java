package dev.vfyjxf.cumulus.impl.design3.runtime;

import dev.vfyjxf.cumulus.impl.design3.codec.RuntimeAssetReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class RuntimeAssetReloadWiring {
    private final RuntimeAssetReloadListener listener;

    public RuntimeAssetReloadWiring() {
        this(new RuntimeAssetReloadListener());
    }

    public RuntimeAssetReloadWiring(RuntimeAssetReloadListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Runtime asset reload listener must not be null");
        }
        this.listener = listener;
    }

    public void register(IEventBus eventBus) {
        if (eventBus == null) {
            throw new IllegalArgumentException("Event bus must not be null");
        }
        eventBus.addListener(AddReloadListenerEvent.class, this::addReloadListener);
    }

    public RuntimeAssetReloadListener listener() {
        return listener;
    }

    private void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(listener);
    }
}
