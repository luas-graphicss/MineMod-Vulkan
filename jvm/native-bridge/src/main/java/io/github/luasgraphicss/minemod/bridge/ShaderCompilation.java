package io.github.luasgraphicss.minemod.bridge;

import java.util.Objects;

public final class ShaderCompilation {
    private static final int[] NO_WORDS = new int[0];

    private final boolean succeeded;
    private final int[] vertexWords;
    private final int[] fragmentWords;
    private final String log;

    private ShaderCompilation(boolean succeeded, int[] vertexWords, int[] fragmentWords, String log) {
        this.succeeded = succeeded;
        this.vertexWords = vertexWords.clone();
        this.fragmentWords = fragmentWords.clone();
        this.log = Objects.requireNonNull(log, "log");
    }

    public static ShaderCompilation succeeded(int[] vertexWords, int[] fragmentWords, String log) {
        return new ShaderCompilation(true, vertexWords, fragmentWords, log);
    }

    public static ShaderCompilation failed(String log) {
        return new ShaderCompilation(false, NO_WORDS, NO_WORDS, log);
    }

    public boolean succeeded() {
        return succeeded;
    }

    public int[] vertexWords() {
        return vertexWords.clone();
    }

    public int[] fragmentWords() {
        return fragmentWords.clone();
    }

    public String log() {
        return log;
    }
}
