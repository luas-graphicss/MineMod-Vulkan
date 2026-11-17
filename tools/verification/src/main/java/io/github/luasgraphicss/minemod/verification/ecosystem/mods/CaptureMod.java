package io.github.luasgraphicss.minemod.verification.ecosystem.mods;

import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Priority;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.ModContext;

public final class CaptureMod {
    private CaptureMod() {}

    public static void install(ModContext mod) {
        mod.bus().on(FramePoint.FRAME_END, Priority.LOWEST, true, canceled -> {
            if (mod.clock().presentedFrame() != mod.clock().frame()) {
                mod.fail("captured frame " + mod.clock().presentedFrame() + " while frame " + mod.clock().frame()
                        + " was being shown");
            }
            mod.note("captures the presented frame");
            return false;
        });
    }
}
