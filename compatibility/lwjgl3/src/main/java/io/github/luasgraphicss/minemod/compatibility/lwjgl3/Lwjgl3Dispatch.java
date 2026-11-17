package io.github.luasgraphicss.minemod.compatibility.lwjgl3;

import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.CapabilityProbe;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.FunctionLookup;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.LookupInstaller;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.MemoryUtil;

public final class Lwjgl3Dispatch implements LookupInstaller, CapabilityProbe {
    public Lwjgl3Dispatch() {
        Configuration.OPENGL_EXPLICIT_INIT.set(true);
    }

    @Override
    public boolean capabilitiesExist(BindingGeneration generation) {
        requireLwjgl3(generation);
        return GL.getFunctionProvider() != null;
    }

    @Override
    public void install(BindingGeneration generation, FunctionLookup lookup) {
        requireLwjgl3(generation);
        GL.create(new LookupProvider(lookup));
    }

    public void uninstall() {
        GL.setCapabilities(null);
        GL.destroy();
    }

    private static void requireLwjgl3(BindingGeneration generation) {
        if (generation != BindingGeneration.LWJGL3) {
            throw new IllegalArgumentException("this binding serves LWJGL 3, not " + generation);
        }
    }

    private static final class LookupProvider implements FunctionProvider {
        private final FunctionLookup lookup;

        private LookupProvider(FunctionLookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public long getFunctionAddress(ByteBuffer functionName) {
            return lookup.addressOf(MemoryUtil.memASCII(MemoryUtil.memAddress(functionName)));
        }
    }
}
