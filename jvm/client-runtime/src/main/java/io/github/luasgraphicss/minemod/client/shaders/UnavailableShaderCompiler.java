package io.github.luasgraphicss.minemod.client.shaders;

import io.github.luasgraphicss.minemod.bridge.CoreShaderSource;
import io.github.luasgraphicss.minemod.bridge.ShaderCompilation;

public final class UnavailableShaderCompiler implements ShaderCompiler {
    private final String reason;

    public UnavailableShaderCompiler(String reason) {
        this.reason = reason;
    }

    @Override
    public ShaderCompilation compile(CoreShaderSource shader) {
        return ShaderCompilation.failed(reason);
    }

    @Override
    public void close() {}
}
