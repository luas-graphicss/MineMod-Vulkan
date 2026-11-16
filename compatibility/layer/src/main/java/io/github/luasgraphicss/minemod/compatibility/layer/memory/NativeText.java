package io.github.luasgraphicss.minemod.compatibility.layer.memory;

import java.nio.charset.StandardCharsets;

public final class NativeText {
    private static final int LONGEST_TEXT = 1 << 20;

    private NativeText() {
    }

    public static String read(NativeMemory memory, long address) {
        if (address == 0) {
            throw new IllegalArgumentException("there is no text at a null address");
        }
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < LONGEST_TEXT; index++) {
            byte character = memory.readByte(address + index);
            if (character == 0) {
                return text.toString();
            }
            text.append((char) (character & 0xFF));
        }
        throw new IllegalArgumentException("the text at " + Long.toHexString(address) + " never ends");
    }

    public static long allocate(NativeMemory memory, String text) {
        byte[] characters = text.getBytes(StandardCharsets.US_ASCII);
        long address = memory.allocate(characters.length + 1);
        for (int index = 0; index < characters.length; index++) {
            memory.writeByte(address + index, characters[index]);
        }
        memory.writeByte(address + characters.length, (byte) 0);
        return address;
    }
}
