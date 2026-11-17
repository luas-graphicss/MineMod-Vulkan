package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import io.github.luasgraphicss.minemod.adapters.events.resources.ResourceGenerations;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus;
import java.util.Map;
import java.util.TreeMap;

public final class ModContext {
    private final String modId;
    private final SessionServices services;
    private final Map<String, Integer> journal = new TreeMap<>();

    ModContext(String modId, SessionServices services) {
        this.modId = modId;
        this.services = services;
    }

    public String modId() {
        return modId;
    }

    public FixtureBus bus() {
        return services.bus;
    }

    public SessionClock clock() {
        return services.clock;
    }

    public ResourceLedger ledger() {
        return services.ledger;
    }

    public ResourceGenerations generations() {
        return services.generations;
    }

    public void onReload(Runnable hook) {
        services.reloadHooks.add(hook);
    }

    public void onWorldUnload(Runnable hook) {
        services.worldUnloadHooks.add(hook);
    }

    public void note(String action) {
        journal.merge(action, 1, Integer::sum);
    }

    public void fail(String failure) {
        services.failures.add(modId + ": " + failure);
    }

    Map<String, Integer> journal() {
        return Map.copyOf(journal);
    }
}
