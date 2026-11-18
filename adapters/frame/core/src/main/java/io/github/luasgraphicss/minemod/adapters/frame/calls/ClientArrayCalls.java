package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.ArrayPointer;
import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrame;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import java.nio.ByteBuffer;
import java.util.Optional;

public final class ClientArrayCalls {
    private static final int NORMAL_VALUES = 3;

    private ClientArrayCalls() {}

    public static void glEnableClientState(int array) {
        GameFrames.current().arrays().enable(array, true);
    }

    public static void glDisableClientState(int array) {
        GameFrames.current().arrays().enable(array, false);
    }

    public static void glVertexPointer(int size, int type, int stride, ByteBuffer memory) {
        GameFrames.current().arrays().vertices(ArrayPointer.inMemory(size, type, stride, memory));
    }

    public static void glVertexPointer(int size, int type, int stride, long offset) {
        GameFrame frame = GameFrames.current();
        frame.arrays().vertices(inBoundBuffer(frame, size, type, stride, offset));
    }

    public static void glColorPointer(int size, int type, int stride, ByteBuffer memory) {
        GameFrames.current().arrays().colors(ArrayPointer.inMemory(size, type, stride, memory));
    }

    public static void glColorPointer(int size, int type, int stride, long offset) {
        GameFrame frame = GameFrames.current();
        frame.arrays().colors(inBoundBuffer(frame, size, type, stride, offset));
    }

    public static void glTexCoordPointer(int size, int type, int stride, ByteBuffer memory) {
        GameFrames.current().arrays().coordinates(ArrayPointer.inMemory(size, type, stride, memory));
    }

    public static void glTexCoordPointer(int size, int type, int stride, long offset) {
        GameFrame frame = GameFrames.current();
        frame.arrays().coordinates(inBoundBuffer(frame, size, type, stride, offset));
    }

    public static void glNormalPointer(int type, int stride, ByteBuffer memory) {
        GameFrames.current().arrays().normals(ArrayPointer.inMemory(NORMAL_VALUES, type, stride, memory));
    }

    public static void glNormalPointer(int type, int stride, long offset) {
        GameFrame frame = GameFrames.current();
        frame.arrays().normals(inBoundBuffer(frame, NORMAL_VALUES, type, stride, offset));
    }

    public static void glDrawArrays(int mode, int first, int count) {
        if (count <= 0) {
            return;
        }
        GameFrame frame = GameFrames.current();
        Optional<Geometry> geometry = frame.arrays().geometry(mode, first, count, frame.buffers());
        if (geometry.isPresent()) {
            frame.draw(geometry.get());
        } else {
            frame.leaveOut();
        }
    }

    private static ArrayPointer inBoundBuffer(GameFrame frame, int size, int type, int stride, long offset) {
        return ArrayPointer.inBufferObject(size, type, stride, frame.buffers().boundArray(), offset);
    }
}
