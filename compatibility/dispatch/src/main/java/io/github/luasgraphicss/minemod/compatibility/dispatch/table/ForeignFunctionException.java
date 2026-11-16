package io.github.luasgraphicss.minemod.compatibility.dispatch.table;

public final class ForeignFunctionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ForeignFunctionException(String function) {
        super(function + " resolved outside the provider library, so another OpenGL driver would serve it");
    }
}
