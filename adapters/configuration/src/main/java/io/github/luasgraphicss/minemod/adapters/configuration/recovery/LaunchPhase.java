package io.github.luasgraphicss.minemod.adapters.configuration.recovery;

public enum LaunchPhase {
    STARTED,
    FIRST_FRAME,
    CLEAN_EXIT;

    public boolean presentedAFrame() {
        return this != STARTED;
    }
}
