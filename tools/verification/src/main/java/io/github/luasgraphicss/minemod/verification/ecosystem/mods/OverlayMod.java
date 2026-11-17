package io.github.luasgraphicss.minemod.verification.ecosystem.mods;

import io.github.luasgraphicss.minemod.adapters.events.dispatch.ElementPhase;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Priority;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.ModContext;

public final class OverlayMod {
    public static final String REPLACED_ELEMENT = "hotbar";

    private OverlayMod() {}

    public static void install(ModContext mod) {
        mod.bus().onElement(
                FramePoint.HUD_ELEMENTS, REPLACED_ELEMENT, ElementPhase.BEFORE, Priority.HIGH, false, canceled -> {
                    mod.note("draws its own " + REPLACED_ELEMENT);
                    return true;
                });
        mod.bus().on(FramePoint.AFTER_HUD, Priority.NORMAL, false, canceled -> {
            mod.note("draws its tooltips");
            return false;
        });
    }
}
