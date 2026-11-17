package io.github.luasgraphicss.minemod.adapters.presentation.display;

import org.lwjgl.LWJGLException;

final class DisplayFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final LWJGLException reason;

    DisplayFailure(LWJGLException reason) {
        super(reason);
        this.reason = reason;
    }

    LWJGLException reason() {
        return reason;
    }
}
