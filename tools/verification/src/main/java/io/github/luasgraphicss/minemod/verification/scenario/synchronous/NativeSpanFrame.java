package io.github.luasgraphicss.minemod.verification.scenario.synchronous;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedLayer;

public final class NativeSpanFrame {
    public static final String QUERY_RESULT_FUNCTION = "glGetQueryObjectuiv";

    private static final long GL_POINTS = 0x0000;
    private static final long GL_FRONT = 0x0404;
    private static final long GL_QUERY_RESULT = 0x8866;
    private static final long GL_SAMPLES_PASSED = 0x8914;
    private static final long PROVIDER_ONLY_RESULT = 99;

    private NativeSpanFrame() {}

    public static long play(SimulatedLayer layer) {
        layer.provider().answer(QUERY_RESULT_FUNCTION, PROVIDER_ONLY_RESULT);
        layer.beginFrame();
        layer.enterCallback();
        int query = layer.generate(ObjectKind.QUERY, 1)[0];
        layer.call("glBeginQuery", GL_SAMPLES_PASSED, query);
        layer.call("glBeginTransformFeedback", GL_POINTS);
        layer.call("glDrawArrays", GL_POINTS, 0, 3);
        layer.exitCallback();
        layer.nativePass("entities");
        layer.enterCallback();
        layer.call("glEndTransformFeedback");
        layer.call("glEndQuery", GL_SAMPLES_PASSED);
        long result = layer.call(QUERY_RESULT_FUNCTION, query, GL_QUERY_RESULT, 0);
        layer.call("glDrawBuffer", GL_FRONT);
        layer.exitCallback();
        layer.present();
        return result;
    }
}
