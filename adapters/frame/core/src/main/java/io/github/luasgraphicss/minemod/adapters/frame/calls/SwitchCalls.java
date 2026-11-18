package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;

public final class SwitchCalls {
    private SwitchCalls() {}

    public static void glEnable(int capability) {
        GameFrames.run(frame -> frame.pipeline().enable(capability, true));
    }

    public static void glDisable(int capability) {
        GameFrames.run(frame -> frame.pipeline().enable(capability, false));
    }

    public static void glPushAttrib(int savedGroups) {
        GameFrames.run(frame -> frame.pipeline().saveSteps());
    }

    public static void glPopAttrib() {
        GameFrames.run(frame -> frame.pipeline().restoreSteps());
    }
}
