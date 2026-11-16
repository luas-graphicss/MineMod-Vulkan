package io.github.luasgraphicss.minemod.client.reload;

import io.github.luasgraphicss.minemod.bridge.SceneReload;

public final class SessionProgramStaging implements ProgramStaging {
    private final SceneReload reload;

    public SessionProgramStaging(SceneReload reload) {
        this.reload = reload;
    }

    @Override
    public void begin() {
        reload.begin();
    }

    @Override
    public boolean stage(int program, int[] vertexWords, int[] fragmentWords) {
        return reload.stageProgram(program, vertexWords, fragmentWords);
    }

    @Override
    public boolean commit() {
        return reload.commit();
    }

    @Override
    public void abort() {
        reload.abort();
    }

    @Override
    public String report() {
        return reload.report();
    }
}
