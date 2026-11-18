package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrame;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;

public final class ImmediateCalls {
    private ImmediateCalls() {}

    public static void glBegin(int mode) {
        GameFrames.current().immediate().begin(mode);
    }

    public static void glTexCoord2f(float horizontal, float vertical) {
        GameFrames.current().immediate().coordinates(horizontal, vertical);
    }

    public static void glVertex3f(float x, float y, float z) {
        GameFrames.current().immediate().vertex(x, y, z);
    }

    public static void glEnd() {
        GameFrame frame = GameFrames.current();
        frame.immediate().end().ifPresent(frame::draw);
    }
}
