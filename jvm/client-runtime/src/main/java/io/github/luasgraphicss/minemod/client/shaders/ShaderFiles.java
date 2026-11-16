package io.github.luasgraphicss.minemod.client.shaders;

import java.util.Optional;

public interface ShaderFiles {
    Optional<ShaderFile> open(String path);
}
