package io.github.luasgraphicss.minemod.compatibility.natives.binding;

import io.github.luasgraphicss.minemod.compatibility.dispatch.install.FunctionLookup;
import java.nio.file.Path;

public final class NativeBinding {
    private NativeBinding() {
    }

    public static void load(Path library) {
        System.load(library.toAbsolutePath().toString());
    }

    public static native long openProvider(
            String directory, String[] dependencies, String entry, String[] settingNames, String[] settingValues);

    public static native void closeProvider(long provider);

    public static native long[] imageSegments(long provider);

    public static native long functionAddress(long provider, String function);

    public static native long createContext(long provider, boolean coreProfile, int major, int minor, long sharedWith);

    public static native void destroyContext(long provider, long context);

    public static native void makeCurrent(long provider, long context);

    public static native String[] callNames();

    public static native long thunkAddress(int function);

    public static native long invoke(long provider, int function, long[] words);

    public static native long unansweredCallCount();

    public static native long allocate(long bytes);

    public static native void free(long address);

    public static native int readInt(long address);

    public static native void writeInt(long address, int value);

    public static native long readLong(long address);

    public static native void writeLong(long address, long value);

    public static native byte readByte(long address);

    public static native void writeByte(long address, byte value);

    public static native void bindLwjgl2Lookup(Class<?> glContext, FunctionLookup lookup);

    public static native void unbindLwjgl2Lookup();
}
