package io.github.luasgraphicss.minemod.client.reload;

import java.util.Objects;

public final class PreparedProgram {
    private final String shader;
    private final int[] vertexWords;
    private final int[] fragmentWords;

    PreparedProgram(String shader, int[] vertexWords, int[] fragmentWords) {
        this.shader = Objects.requireNonNull(shader, "shader");
        this.vertexWords = vertexWords.clone();
        this.fragmentWords = fragmentWords.clone();
    }

    public String shader() {
        return shader;
    }

    int[] vertexWords() {
        return vertexWords;
    }

    int[] fragmentWords() {
        return fragmentWords;
    }
}
