package io.github.luasgraphicss.minemod.client.compatibility;

public final class ModOpenGlUnavailable extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ModOpenGlUnavailable(String operation, Throwable cause) {
        super(operation + " failed: " + cause.getMessage(), cause);
    }
}
