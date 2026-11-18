package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import java.nio.ByteBuffer;

public final class BufferCalls {
    private BufferCalls() {}

    public static int glGenBuffers() {
        return GameFrames.current().buffers().generate();
    }

    public static int glGenBuffersARB() {
        return glGenBuffers();
    }

    public static void glBindBuffer(int target, int buffer) {
        GameFrames.current().buffers().bind(target, buffer);
    }

    public static void glBindBufferARB(int target, int buffer) {
        glBindBuffer(target, buffer);
    }

    public static void glBufferData(int target, ByteBuffer contents, int usage) {
        GameFrames.current().buffers().store(target, contents);
    }

    public static void glBufferDataARB(int target, ByteBuffer contents, int usage) {
        glBufferData(target, contents, usage);
    }

    public static void glDeleteBuffers(int buffer) {
        GameFrames.current().buffers().delete(buffer);
    }

    public static void glDeleteBuffersARB(int buffer) {
        glDeleteBuffers(buffer);
    }
}
