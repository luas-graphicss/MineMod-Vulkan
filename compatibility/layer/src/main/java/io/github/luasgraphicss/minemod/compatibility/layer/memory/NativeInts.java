package io.github.luasgraphicss.minemod.compatibility.layer.memory;

public final class NativeInts {
    private NativeInts() {
    }

    public static int[] read(NativeMemory memory, long address, int count) {
        int[] values = new int[Math.max(count, 0)];
        for (int index = 0; index < values.length; index++) {
            values[index] = memory.readInt(address + (long) index * Integer.BYTES);
        }
        return values;
    }

    public static void write(NativeMemory memory, long address, int[] values) {
        for (int index = 0; index < values.length; index++) {
            memory.writeInt(address + (long) index * Integer.BYTES, values[index]);
        }
    }
}
