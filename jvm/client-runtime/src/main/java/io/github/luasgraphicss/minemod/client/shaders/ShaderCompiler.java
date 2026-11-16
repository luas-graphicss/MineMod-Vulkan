package io.github.luasgraphicss.minemod.client.shaders;

import io.github.luasgraphicss.minemod.bridge.CoreShaderSource;
import io.github.luasgraphicss.minemod.bridge.ShaderCompilation;

public interface ShaderCompiler extends AutoCloseable {
    ShaderCompilation compile(CoreShaderSource shader);

    @Override
    void close();
}
