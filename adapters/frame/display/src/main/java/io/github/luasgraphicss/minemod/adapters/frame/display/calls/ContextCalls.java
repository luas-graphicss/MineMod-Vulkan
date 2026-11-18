package io.github.luasgraphicss.minemod.adapters.frame.display.calls;

import io.github.luasgraphicss.minemod.adapters.frame.answers.BlankInstances;
import io.github.luasgraphicss.minemod.adapters.frame.answers.OfferedCapabilities;
import org.lwjgl.opengl.ContextCapabilities;

public final class ContextCalls {
    private ContextCalls() {}

    public static ContextCapabilities getCapabilities() {
        return Offered.CAPABILITIES;
    }

    private static final class Offered {
        static final ContextCapabilities CAPABILITIES =
                BlankInstances.raising(ContextCapabilities.class, OfferedCapabilities.flags());
    }
}
