package io.github.luasgraphicss.minemod.verification.ecosystem.mods;

import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Priority;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.ModContext;
import java.util.List;

public final class MapMod {
    private final ModContext mod;
    private final List<String> targets;
    private boolean allocated;

    private MapMod(ModContext mod) {
        this.mod = mod;
        this.targets = List.of(mod.modId() + " framebuffer", mod.modId() + " texture");
    }

    public static void install(ModContext mod) {
        MapMod map = new MapMod(mod);
        mod.bus().on(FramePoint.AFTER_HUD, Priority.NORMAL, false, canceled -> map.draw());
        mod.onWorldUnload(map::release);
    }

    private boolean draw() {
        if (!allocated) {
            targets.forEach(mod.ledger()::create);
            allocated = true;
            mod.note("allocates the map target");
        }
        mod.note("updates and reads back the map");
        return false;
    }

    private void release() {
        if (allocated) {
            targets.forEach(mod.ledger()::destroy);
            allocated = false;
            mod.note("releases the map target");
        }
    }
}
