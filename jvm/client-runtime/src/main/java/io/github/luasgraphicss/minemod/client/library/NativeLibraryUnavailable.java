package io.github.luasgraphicss.minemod.client.library;

public final class NativeLibraryUnavailable extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NativeLibraryUnavailable(String reason) {
        super(reason);
    }

    public NativeLibraryUnavailable(String operation, Throwable cause) {
        super(operation + " failed", cause);
    }
}
