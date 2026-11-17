package io.github.luasgraphicss.minemod.adapters.shaderpacks.targets;

public enum BufferSide {
    MAIN,
    ALT;

    public BufferSide other() {
        return this == MAIN ? ALT : MAIN;
    }
}
