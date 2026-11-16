package io.github.luasgraphicss.minemod.bridge;

import java.nio.ByteBuffer;

final class DirectBuffers {
    private DirectBuffers() {}

    static void requireDirect(ByteBuffer buffer, String role) {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("the " + role + " buffer must be direct to cross into native code");
        }
    }
}
