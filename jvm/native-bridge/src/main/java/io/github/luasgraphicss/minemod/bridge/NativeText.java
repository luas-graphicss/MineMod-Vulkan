package io.github.luasgraphicss.minemod.bridge;

import java.nio.charset.StandardCharsets;

final class NativeText {
    private NativeText() {}

    static String decode(byte[] bytes) {
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    static byte[] encode(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
