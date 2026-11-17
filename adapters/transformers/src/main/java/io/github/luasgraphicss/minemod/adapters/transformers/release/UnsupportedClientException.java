package io.github.luasgraphicss.minemod.adapters.transformers.release;

import java.util.List;

public final class UnsupportedClientException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String clientSha1;

    UnsupportedClientException(String clientSha1, List<String> supportedReleases) {
        super("the client with SHA-1 " + clientSha1 + " is not one of the supported releases " + supportedReleases
                + "; it was left untouched");
        this.clientSha1 = clientSha1;
    }

    public String clientSha1() {
        return clientSha1;
    }
}
