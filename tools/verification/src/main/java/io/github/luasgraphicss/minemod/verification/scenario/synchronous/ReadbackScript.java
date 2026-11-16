package io.github.luasgraphicss.minemod.verification.scenario.synchronous;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.GlDriver;
import java.util.List;

final class ReadbackScript {
    static final List<Long> BYTES_PER_ALIGNMENT = List.of(18L, 24L);

    private static final long GL_PACK_ALIGNMENT = 0x0D05;
    private static final long GL_UNSIGNED_BYTE = 0x1401;
    private static final long GL_RGB = 0x1907;
    private static final long GL_NEAREST = 0x2600;
    private static final long GL_COLOR_BUFFER_BIT = 0x4000;
    private static final long GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final long GL_FRAMEBUFFER = 0x8D40;
    private static final long DEFAULT_FRAMEBUFFER = 0;
    private static final long TIGHTLY_PACKED = 1;
    private static final long WORD_ALIGNED = 4;

    private ReadbackScript() {}

    static List<Long> play(GlDriver gl) {
        int copy = gl.generate(ObjectKind.FRAMEBUFFER, 1)[0];
        gl.call("glBindFramebuffer", GL_DRAW_FRAMEBUFFER, copy);
        gl.call("glBlitFramebuffer", 8, 8, 24, 24, 0, 0, 16, 16, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, DEFAULT_FRAMEBUFFER);
        return List.of(readThreeByTwo(gl, TIGHTLY_PACKED), readThreeByTwo(gl, WORD_ALIGNED));
    }

    private static long readThreeByTwo(GlDriver gl, long alignment) {
        gl.call("glPixelStorei", GL_PACK_ALIGNMENT, alignment);
        return gl.call("glReadPixels", 0, 0, 3, 2, GL_RGB, GL_UNSIGNED_BYTE, 0);
    }
}
