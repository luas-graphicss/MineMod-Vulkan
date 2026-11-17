package io.github.luasgraphicss.minemod.compatibility.layer.fixtures;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecordingProvider implements ProviderCalls {
    public static final int FIRST_NAME = 1;
    private static final int GL_NO_ERROR = 0;
    private static final int COUNT = 0;
    private static final int NAMES = 1;

    private final NativeMemory memory;
    private final List<String> events = new ArrayList<>();
    private final Deque<Integer> errors = new ArrayDeque<>();
    private final Map<String, Integer> refusals = new HashMap<>();
    private int nextName = FIRST_NAME;

    public RecordingProvider(NativeMemory memory) {
        this.memory = memory;
    }

    @Override
    public long invoke(String function, long[] arguments) {
        if (function.equals("glGetError")) {
            return errors.isEmpty() ? GL_NO_ERROR : errors.remove();
        }
        if (function.startsWith("glGen") || function.startsWith("glCreate")) {
            events.add(function + "[" + arguments[COUNT] + "] -> " + nextName);
            writeNames(arguments[NAMES], (int) arguments[COUNT]);
            return 0;
        }
        events.add(function + Arrays.toString(arguments));
        Integer refusal = refusals.get(function);
        if (refusal != null) {
            errors.add(refusal);
        }
        return 0;
    }

    public int takeError() {
        return (int) invoke("glGetError", new long[0]);
    }

    public void refuse(String function, int error) {
        refusals.put(function, error);
    }

    public List<String> events() {
        return events;
    }

    private void writeNames(long address, int count) {
        for (int index = 0; index < count; index++) {
            memory.writeInt(address + (long) index * Integer.BYTES, nextName++);
        }
    }
}
