package io.github.luasgraphicss.minemod.compatibility.natives.gate;

import io.github.luasgraphicss.minemod.compatibility.natives.binding.CallTable;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import java.util.Objects;

public final class CallGate {
    private static final ThreadLocal<CallInterceptor> BOUND = new ThreadLocal<>();

    private CallGate() {
    }

    public static void bind(CallInterceptor interceptor) {
        BOUND.set(Objects.requireNonNull(interceptor, "interceptor"));
    }

    public static void unbind() {
        BOUND.remove();
    }

    public static long unansweredCallCount() {
        return NativeBinding.unansweredCallCount();
    }

    static long intercept(int function, long[] words) {
        String name = CallTable.loaded().nameOf(function);
        CallInterceptor interceptor = BOUND.get();
        if (interceptor == null) {
            throw new IllegalStateException(name + " was called on a thread where no compatibility context is current");
        }
        return interceptor.call(name, words);
    }
}
