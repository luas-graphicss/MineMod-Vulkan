package io.github.luasgraphicss.minemod.verification.differential;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;

public record ScriptStep(Action action, ObjectKind kind, int slot, long value) {
    public enum Action {
        GENERATE,
        BIND,
        BIND_NOTHING,
        BIND_UNGENERATED,
        DELETE,
        IS_OBJECT,
        ENABLE,
        DISABLE,
        DRAW,
        READ,
        IMMEDIATE_TRIANGLE,
        GET_ERROR
    }
}
