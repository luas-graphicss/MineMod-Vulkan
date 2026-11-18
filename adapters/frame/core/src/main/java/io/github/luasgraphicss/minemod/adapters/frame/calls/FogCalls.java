package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import java.nio.FloatBuffer;

public final class FogCalls {
    private FogCalls() {}

    public static void glFogi(int name, int value) {
        GameFrames.run(frame -> frame.pipeline().fog().parameter(name, value));
    }

    public static void glFogf(int name, float value) {
        GameFrames.run(frame -> frame.pipeline().fog().parameter(name, value));
    }

    public static void glFog(int name, FloatBuffer values) {
        FloatBuffer kept = CallArguments.copyOf(values);
        GameFrames.run(frame -> frame.pipeline().fog().parameter(name, kept));
    }

    public static void glFogfv(int name, FloatBuffer values) {
        glFog(name, values);
    }
}
