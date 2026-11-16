package io.github.luasgraphicss.minemod.compatibility.bridges.registry;

import io.github.luasgraphicss.minemod.compatibility.spi.BridgeDescriptor;
import io.github.luasgraphicss.minemod.compatibility.spi.RenderBridge;
import io.github.luasgraphicss.minemod.compatibility.spi.SpiVersion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

public final class BridgeRegistry {
    private final Map<String, RenderBridge> bridgesById = new LinkedHashMap<>();
    private final Map<String, RenderBridge> bridgesByOperation = new LinkedHashMap<>();

    public synchronized void register(RenderBridge bridge) {
        BridgeDescriptor descriptor = bridge.descriptor();
        if (!SpiVersion.accepts(descriptor.spiVersion())) {
            throw new IllegalArgumentException("bridge " + descriptor.id() + " was built for SPI version "
                    + descriptor.spiVersion() + ", this runtime speaks " + SpiVersion.CURRENT);
        }
        if (bridgesById.containsKey(descriptor.id())) {
            throw new IllegalArgumentException("bridge " + descriptor.id() + " is already registered");
        }
        for (String operation : descriptor.operations()) {
            RenderBridge owner = bridgesByOperation.get(operation);
            if (owner != null) {
                throw new IllegalArgumentException("bridges " + owner.descriptor().id() + " and " + descriptor.id()
                        + " both claim \"" + operation + "\"");
            }
        }
        bridgesById.put(descriptor.id(), bridge);
        for (String operation : descriptor.operations()) {
            bridgesByOperation.put(operation, bridge);
        }
    }

    public List<String> discover(ClassLoader loader) {
        List<String> refused = new ArrayList<>();
        for (RenderBridge bridge : ServiceLoader.load(RenderBridge.class, loader)) {
            try {
                register(bridge);
            } catch (IllegalArgumentException refusal) {
                refused.add(refusal.getMessage());
            }
        }
        return refused;
    }

    public synchronized Optional<RenderBridge> bridgeFor(String operation) {
        return Optional.ofNullable(bridgesByOperation.get(operation));
    }

    public synchronized List<RenderBridge> bridges() {
        return new ArrayList<>(bridgesById.values());
    }

    public synchronized Set<String> unserved(Collection<String> operations) {
        Set<String> unserved = new TreeSet<>(operations);
        unserved.removeAll(bridgesByOperation.keySet());
        return unserved;
    }
}
