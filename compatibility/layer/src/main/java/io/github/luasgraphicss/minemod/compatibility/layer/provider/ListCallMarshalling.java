package io.github.luasgraphicss.minemod.compatibility.layer.provider;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class ListCallMarshalling implements ProviderCalls {
    private static final String CALL_LISTS = "glCallLists";
    private static final int GL_UNSIGNED_INT = 0x1405;
    private final ProviderCalls calls;
    private final NativeMemory memory;

    public ListCallMarshalling(ProviderCalls calls, NativeMemory memory) {
        this.calls = Objects.requireNonNull(calls, "calls");
        this.memory = Objects.requireNonNull(memory, "memory");
    }

    @Override
    public long invoke(String function, long[] arguments) {
        if (!function.equals(CALL_LISTS)) {
            return calls.invoke(function, arguments);
        }
        if (arguments.length == 0) {
            return 0;
        }
        long lists = memory.allocate(arguments.length * Integer.BYTES);
        try {
            for (int index = 0; index < arguments.length; index++) {
                memory.writeInt(lists + (long) index * Integer.BYTES, (int) arguments[index]);
            }
            return calls.invoke(CALL_LISTS, new long[] {arguments.length, GL_UNSIGNED_INT, lists});
        } finally {
            memory.free(lists);
        }
    }
}
