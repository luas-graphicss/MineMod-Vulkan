package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.pipeline.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import java.nio.FloatBuffer;

public final class LightCalls {
    private LightCalls() {}

    public static void glLight(int light, int name, FloatBuffer values) {
        FloatBuffer kept = CallArguments.copyOf(values);
        GameFrames.run(frame -> {
            FixedPipeline pipeline = frame.pipeline();
            pipeline.lights().parameter(light, name, kept, pipeline.matrices().modelView().current());
        });
    }

    public static void glLightfv(int light, int name, FloatBuffer values) {
        glLight(light, name, values);
    }
}
