package io.github.luasgraphicss.minemod.adapters.frame.calls;

import java.nio.FloatBuffer;

final class CallArguments {
    private CallArguments() {}

    static FloatBuffer copyOf(FloatBuffer values) {
        FloatBuffer kept = FloatBuffer.allocate(values.remaining());
        kept.put(values.duplicate());
        kept.flip();
        return kept;
    }
}
