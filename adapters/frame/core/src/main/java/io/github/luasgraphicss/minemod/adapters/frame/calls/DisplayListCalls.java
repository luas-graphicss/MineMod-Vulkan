package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;

public final class DisplayListCalls {
    private DisplayListCalls() {}

    public static int glGenLists(int range) {
        return GameFrames.current().lists().generate(range);
    }

    public static void glDeleteLists(int first, int range) {
        GameFrames.current().lists().delete(first, range);
    }

    public static void glNewList(int list, int mode) {
        GameFrames.current().lists().begin(list);
    }

    public static void glEndList() {
        GameFrames.current().lists().end();
    }

    public static void glCallList(int list) {
        GameFrames.run(frame -> frame.call(list));
    }
}
