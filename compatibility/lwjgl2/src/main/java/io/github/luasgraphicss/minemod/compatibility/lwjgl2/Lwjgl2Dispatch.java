package io.github.luasgraphicss.minemod.compatibility.lwjgl2;

import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.CapabilityProbe;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.FunctionLookup;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.LookupInstaller;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import java.lang.reflect.Field;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GLContext;

public final class Lwjgl2Dispatch implements LookupInstaller, CapabilityProbe {
    private static final String LIBRARY_LOADS_FIELD = "gl_ref_count";

    @Override
    public boolean capabilitiesExist(BindingGeneration generation) {
        requireLwjgl2(generation);
        return openGlLibraryLoads() > 0;
    }

    @Override
    public void install(BindingGeneration generation, FunctionLookup lookup) {
        requireLwjgl2(generation);
        NativeBinding.bindLwjgl2Lookup(GLContext.class, lookup);
    }

    public void uninstall() {
        try {
            GLContext.useContext(null);
        } catch (LWJGLException failure) {
            throw new IllegalStateException("LWJGL 2 could not drop the capabilities of this thread", failure);
        }
        NativeBinding.unbindLwjgl2Lookup();
    }

    private static int openGlLibraryLoads() {
        try {
            Field loads = GLContext.class.getDeclaredField(LIBRARY_LOADS_FIELD);
            loads.setAccessible(true);
            synchronized (GLContext.class) {
                return loads.getInt(null);
            }
        } catch (ReflectiveOperationException unreadable) {
            throw new IllegalStateException(
                    "this LWJGL 2 build does not count the loads of its OpenGL library in " + LIBRARY_LOADS_FIELD,
                    unreadable);
        }
    }

    private static void requireLwjgl2(BindingGeneration generation) {
        if (generation != BindingGeneration.LWJGL2) {
            throw new IllegalArgumentException("this binding serves LWJGL 2, not " + generation);
        }
    }
}
