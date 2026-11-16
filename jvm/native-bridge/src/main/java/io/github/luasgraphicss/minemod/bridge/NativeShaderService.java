package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.abi.AbiVersion;
import java.util.Map;

public final class NativeShaderService implements AutoCloseable {
    private static final long CLOSED = 0;
    private static final int VERTEX_STAGE = 0;
    private static final int FRAGMENT_STAGE = 1;

    private long address;

    private NativeShaderService(long address) {
        this.address = address;
    }

    public static boolean isAvailable() {
        return nativeIsAvailable();
    }

    public static NativeShaderService open(ShaderServiceSetup setup) {
        if (!nativeIsAvailable()) {
            throw new AbiException(
                    AbiResult.UNSUPPORTED_CAPABILITY, "opening a shader service in a library built without one");
        }
        long[] address = new long[1];
        int resultCode = nativeCreate(
                AbiVersion.CURRENT.major(),
                AbiVersion.CURRENT.minor(),
                NativeText.encode(setup.compiler().toString()),
                NativeText.encode(setup.scratchDirectory().toString()),
                setup.wallTimeMilliseconds(),
                setup.compilerMemoryBytes(),
                setup.cacheBytes(),
                address);
        NativeResults.requireOk(resultCode, "opening a shader service with " + setup);
        return new NativeShaderService(address[0]);
    }

    public synchronized ShaderCompilation compile(CoreShaderSource shader) {
        Map<String, String> includes = shader.includes();
        byte[][] keys = new byte[includes.size()][];
        byte[][] texts = new byte[includes.size()][];
        int index = 0;
        for (Map.Entry<String, String> include : includes.entrySet()) {
            keys[index] = NativeText.encode(include.getKey());
            texts[index] = NativeText.encode(include.getValue());
            index++;
        }
        long[] compilation = new long[1];
        int resultCode = nativeCompile(
                openAddress(),
                NativeText.encode(shader.name()),
                NativeText.encode(shader.vertex()),
                NativeText.encode(shader.fragment()),
                keys,
                texts,
                compilation);
        NativeResults.requireOk(resultCode, "compiling the core shader " + shader.name());
        try {
            return read(compilation[0]);
        } finally {
            nativeDestroyCompilation(compilation[0]);
        }
    }

    @Override
    public synchronized void close() {
        if (address == CLOSED) {
            return;
        }
        nativeDestroy(address);
        address = CLOSED;
    }

    private static ShaderCompilation read(long compilation) {
        String log = NativeText.decode(nativeCompilationText(compilation));
        if (!nativeCompilationSucceeded(compilation)) {
            return ShaderCompilation.failed(log);
        }
        int[] vertexWords = nativeCompilationWords(compilation, VERTEX_STAGE);
        int[] fragmentWords = nativeCompilationWords(compilation, FRAGMENT_STAGE);
        if (vertexWords == null || fragmentWords == null) {
            throw new AbiException(AbiResult.OUT_OF_MEMORY, "copying the words of a compiled core shader");
        }
        return ShaderCompilation.succeeded(vertexWords, fragmentWords, log);
    }

    private long openAddress() {
        if (address == CLOSED) {
            throw new IllegalStateException("the shader service is closed");
        }
        return address;
    }

    private static native boolean nativeIsAvailable();

    private static native int nativeCreate(
            int abiMajor,
            int abiMinor,
            byte[] compilerPath,
            byte[] scratchDirectory,
            int wallTimeMilliseconds,
            long compilerMemoryBytes,
            long cacheBytes,
            long[] serviceAddress);

    private static native void nativeDestroy(long service);

    private static native int nativeCompile(
            long service,
            byte[] name,
            byte[] vertex,
            byte[] fragment,
            byte[][] includeKeys,
            byte[][] includeTexts,
            long[] compilationAddress);

    private static native void nativeDestroyCompilation(long compilation);

    private static native boolean nativeCompilationSucceeded(long compilation);

    private static native int[] nativeCompilationWords(long compilation, int stage);

    private static native byte[] nativeCompilationText(long compilation);
}
