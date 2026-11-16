package io.github.luasgraphicss.minemod.client.shaders;

import io.github.luasgraphicss.minemod.bridge.CoreShaderSource;
import io.github.luasgraphicss.minemod.bridge.NativeShaderService;
import io.github.luasgraphicss.minemod.bridge.ShaderCompilation;
import io.github.luasgraphicss.minemod.bridge.ShaderServiceSetup;
import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class NativeShaderCompiler implements ShaderCompiler {
    private final NativeShaderService service;
    private boolean isClosed;

    private NativeShaderCompiler(NativeShaderService service) {
        this.service = service;
    }

    public static ShaderCompiler open(Path scratchDirectory) {
        if (!NativeShaderService.isAvailable()) {
            return new UnavailableShaderCompiler("the native library was built without the shader service");
        }
        Optional<Path> compiler = ShaderCompilerLocator.locate();
        if (!compiler.isPresent()) {
            return new UnavailableShaderCompiler("no glslang compiler was found; install glslangValidator or name one"
                    + " with -D" + ShaderCompilerLocator.PROPERTY);
        }
        try {
            Files.createDirectories(scratchDirectory);
            return new NativeShaderCompiler(
                    NativeShaderService.open(ShaderServiceSetup.of(compiler.get(), scratchDirectory)));
        } catch (IOException | AbiException failure) {
            return new UnavailableShaderCompiler("the shader service could not start: " + failure.getMessage());
        }
    }

    @Override
    public synchronized ShaderCompilation compile(CoreShaderSource shader) {
        if (isClosed) {
            return ShaderCompilation.failed("the shader service has stopped");
        }
        try {
            return service.compile(shader);
        } catch (AbiException refusal) {
            return ShaderCompilation.failed("the shader service refused the sources: " + refusal.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        isClosed = true;
        service.close();
    }
}
