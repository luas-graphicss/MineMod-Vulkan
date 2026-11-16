package io.github.luasgraphicss.minemod.compatibility.provider.load;

public final class ProviderLoadException extends Exception {
    private static final long serialVersionUID = 1L;

    private final Reason reason;

    ProviderLoadException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        NOT_A_FILE_NAME,
        MISSING,
        NOT_A_REGULAR_FILE,
        DIRECTORY_WRITABLE_BY_OTHERS,
        DIGEST_MISMATCH,
        OPEN_FAILED
    }
}
