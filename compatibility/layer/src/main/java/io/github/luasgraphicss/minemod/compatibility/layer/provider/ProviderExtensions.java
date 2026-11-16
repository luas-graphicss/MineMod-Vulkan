package io.github.luasgraphicss.minemod.compatibility.layer.provider;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeText;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.provider.identity.ProviderStrings;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.ArrayList;
import java.util.List;

public final class ProviderExtensions {
    private static final int GL_RENDERER = 0x1F01;
    private static final int GL_VERSION = 0x1F02;
    private static final int GL_EXTENSIONS = 0x1F03;
    private static final int GL_NUM_EXTENSIONS = 0x821D;

    private ProviderExtensions() {
    }

    public static List<String> list(ProviderCalls calls, NativeMemory memory) {
        int count;
        try (ScratchInts scratch = new ScratchInts(memory, 1)) {
            calls.invoke("glGetIntegerv", new long[] {GL_NUM_EXTENSIONS, scratch.address()});
            count = scratch.first();
        }
        List<String> extensions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            extensions.add(NativeText.read(memory, calls.invoke("glGetStringi", new long[] {GL_EXTENSIONS, index})));
        }
        return extensions;
    }

    public static ProviderStrings strings(ProviderCalls calls, NativeMemory memory) {
        return new ProviderStrings(text(calls, memory, GL_RENDERER), text(calls, memory, GL_VERSION));
    }

    private static String text(ProviderCalls calls, NativeMemory memory, int name) {
        return NativeText.read(memory, calls.invoke("glGetString", new long[] {name}));
    }
}
