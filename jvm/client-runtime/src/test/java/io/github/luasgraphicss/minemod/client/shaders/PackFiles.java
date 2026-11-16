package io.github.luasgraphicss.minemod.client.shaders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class PackFiles implements ShaderFiles {
    private final Map<String, ShaderFile> files = new HashMap<>();

    PackFiles builtIn(String path, String text) {
        files.put(path, new ShaderFile(text, false));
        return this;
    }

    PackFiles replaced(String path, String text) {
        files.put(path, new ShaderFile(text, true));
        return this;
    }

    @Override
    public Optional<ShaderFile> open(String path) {
        return Optional.ofNullable(files.get(path));
    }
}
