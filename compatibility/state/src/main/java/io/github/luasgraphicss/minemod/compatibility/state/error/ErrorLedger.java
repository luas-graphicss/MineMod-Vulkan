package io.github.luasgraphicss.minemod.compatibility.state.error;

import java.util.Objects;

public final class ErrorLedger {
    public static final int NO_ERROR = 0;

    private final ProviderErrors provider;
    private int recorded = NO_ERROR;

    public ErrorLedger(ProviderErrors provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public void raise(int error) {
        if (error == NO_ERROR) {
            throw new IllegalArgumentException("raising an error needs an error code");
        }
        if (recorded != NO_ERROR) {
            return;
        }
        int earlier = provider.takeError();
        recorded = earlier != NO_ERROR ? earlier : error;
    }

    public int take() {
        int providerError = provider.takeError();
        if (recorded == NO_ERROR) {
            return providerError;
        }
        int first = recorded;
        recorded = NO_ERROR;
        return first;
    }
}
