package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import io.github.luasgraphicss.minemod.adapters.events.resources.ResourceGenerations;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus;
import java.util.ArrayList;
import java.util.List;

final class SessionServices {
    final FixtureBus bus;
    final ResourceGenerations generations;
    final SessionClock clock = new SessionClock();
    final ResourceLedger ledger = new ResourceLedger();
    final List<Runnable> reloadHooks = new ArrayList<>();
    final List<Runnable> worldUnloadHooks = new ArrayList<>();
    final List<String> failures = new ArrayList<>();

    SessionServices(FixtureBus bus, ResourceGenerations generations) {
        this.bus = bus;
        this.generations = generations;
    }
}
