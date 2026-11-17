package io.github.luasgraphicss.minemod.adapters.configuration.recovery;

public final class RecoveryDecision {
    private final boolean safeMode;
    private final String reason;

    RecoveryDecision(boolean safeMode, String reason) {
        this.safeMode = safeMode;
        this.reason = reason;
    }

    public boolean safeMode() {
        return safeMode;
    }

    public String reason() {
        return reason;
    }
}
