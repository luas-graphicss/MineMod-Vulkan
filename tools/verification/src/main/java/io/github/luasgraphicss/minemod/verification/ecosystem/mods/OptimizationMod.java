package io.github.luasgraphicss.minemod.verification.ecosystem.mods;

import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Priority;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.ModContext;

public final class OptimizationMod {
    private OptimizationMod() {}

    public static void install(ModContext mod) {
        mod.bus().on(FramePoint.FRAME_START, Priority.LOWEST, false, canceled -> {
            mod.note("schedules its work for the frame");
            return false;
        });
        mod.bus().on(FramePoint.FRAME_END, Priority.LOWEST, false, canceled -> {
            mod.note("collects its work of the frame");
            return false;
        });
    }
}
