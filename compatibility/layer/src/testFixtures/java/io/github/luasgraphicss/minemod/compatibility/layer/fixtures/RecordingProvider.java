package io.github.luasgraphicss.minemod.compatibility.layer.fixtures;

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
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;
    private static final int GL_PIXEL_UNPACK_BUFFER_BINDING = 0x88EF;
    private static final int TEXEL_BYTES = 4;

    private final HeapMemory memory;
    private final List<String> events = new ArrayList<>();
    private final List<byte[]> uploads = new ArrayList<>();
    private final Deque<Integer> errors = new ArrayDeque<>();
    private final Map<String, Integer> refusals = new HashMap<>();
    private final Map<Integer, Integer> integers = new HashMap<>();
    private int nextName = FIRST_NAME;

    public RecordingProvider(HeapMemory memory) {
        this.memory = memory;
    }

    @Override
    public long invoke(String function, long[] arguments) {
        switch (function) {
            case "glGetError":
                return errors.isEmpty() ? GL_NO_ERROR : errors.remove();
            case "glGetIntegerv":
                memory.writeInt(arguments[1], integer((int) arguments[0]));
                return 0;
            case "glBindTexture":
                integers.put(GL_TEXTURE_BINDING_2D, (int) arguments[1]);
                break;
            case "glPixelStorei":
                integers.put((int) arguments[0], (int) arguments[1]);
                break;
            case "glBindBuffer":
                if (arguments[0] == GL_PIXEL_UNPACK_BUFFER) {
                    integers.put(GL_PIXEL_UNPACK_BUFFER_BINDING, (int) arguments[1]);
                }
                break;
            case "glTexSubImage2D":
                uploads.add(memory.readBytes(arguments[8], (int) (arguments[4] * arguments[5] * TEXEL_BYTES)));
                break;
            default:
                if (function.startsWith("glGen") || function.startsWith("glCreate")) {
                    return generate(function, (int) arguments[0], arguments[1]);
                }
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

    public void answer(int parameter, int value) {
        integers.put(parameter, value);
    }

    public int integer(int parameter) {
        Integer value = integers.get(parameter);
        return value == null ? 0 : value;
    }

    public List<String> events() {
        return events;
    }

    public List<byte[]> uploads() {
        return uploads;
    }

    private long generate(String function, int count, long names) {
        events.add(function + "[" + count + "] -> " + nextName);
        for (int index = 0; index < count; index++) {
            memory.writeInt(names + (long) index * Integer.BYTES, nextName++);
        }
        return 0;
    }
}
