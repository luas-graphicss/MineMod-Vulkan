package io.github.luasgraphicss.minemod.client.shaders;

import java.util.Objects;

public final class ShaderFile {
    private final String text;
    private final boolean replacesBuiltIn;

    public ShaderFile(String text, boolean replacesBuiltIn) {
        this.text = Objects.requireNonNull(text, "text");
        this.replacesBuiltIn = replacesBuiltIn;
    }

    public String text() {
        return text;
    }

    public boolean replacesBuiltIn() {
        return replacesBuiltIn;
    }
}
