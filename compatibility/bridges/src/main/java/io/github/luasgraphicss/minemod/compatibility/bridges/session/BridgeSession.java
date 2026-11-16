package io.github.luasgraphicss.minemod.compatibility.bridges.session;

import io.github.luasgraphicss.minemod.compatibility.bridges.registry.BridgeRegistry;
import io.github.luasgraphicss.minemod.compatibility.spi.RenderBridge;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BridgeSession {
    private final BridgeRegistry registry;
    private final BridgeHost host;
    private final Map<RenderBridge, ScopedBridgeContext> attached = new LinkedHashMap<>();

    public BridgeSession(BridgeRegistry registry, BridgeHost host) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.host = Objects.requireNonNull(host, "host");
    }

    public synchronized void attachAll() {
        for (RenderBridge bridge : registry.bridges()) {
            if (attached.containsKey(bridge)) {
                continue;
            }
            ScopedBridgeContext context = new ScopedBridgeContext(bridge.descriptor().id(), host);
            try {
                bridge.attach(context);
            } catch (RuntimeException failure) {
                context.revoke();
                throw failure;
            }
            attached.put(bridge, context);
        }
    }

    public synchronized long serve(String operation, long[] arguments) {
        RenderBridge bridge = registry.bridgeFor(operation)
                .filter(attached::containsKey)
                .orElseThrow(() -> new IllegalStateException("no attached bridge serves \"" + operation + "\""));
        return bridge.serve(operation, arguments.clone());
    }

    public synchronized boolean serves(String operation) {
        return registry.bridgeFor(operation).filter(attached::containsKey).isPresent();
    }

    public synchronized List<String> detachAll() {
        List<String> failures = new ArrayList<>();
        List<RenderBridge> newestFirst = new ArrayList<>(attached.keySet());
        for (int index = newestFirst.size() - 1; index >= 0; index--) {
            RenderBridge bridge = newestFirst.get(index);
            try {
                bridge.detach();
            } catch (RuntimeException failure) {
                failures.add(bridge.descriptor().id() + ": " + failure.getMessage());
            } finally {
                attached.remove(bridge).revoke();
            }
        }
        return failures;
    }
}
