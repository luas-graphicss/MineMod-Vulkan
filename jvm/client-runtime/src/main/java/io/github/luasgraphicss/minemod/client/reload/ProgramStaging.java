package io.github.luasgraphicss.minemod.client.reload;

public interface ProgramStaging {
    void begin();

    boolean stage(int program, int[] vertexWords, int[] fragmentWords);

    boolean commit();

    void abort();

    String report();
}
