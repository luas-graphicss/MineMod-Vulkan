package io.github.luasgraphicss.minemod.bridge;

import java.nio.ByteBuffer;

final class DirectStreams {
    private DirectStreams() {}

    static ByteBuffer copyOf(ByteBuffer stream) {
        ByteBuffer direct = ByteBuffer.allocateDirect(stream.remaining());
        direct.put(stream.duplicate());
        direct.flip();
        return direct;
    }

    static byte[] bytesOf(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }
}
