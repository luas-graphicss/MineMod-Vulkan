package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class ProviderOnlyHandoff implements AttachmentHandoff {
    private static final long[] NO_ARGUMENTS = new long[0];
    private final ProviderCalls provider;
    private long releases;

    public ProviderOnlyHandoff(ProviderCalls provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public long releases() {
        return releases;
    }

    @Override
    public void releaseToProvider() {
        releases++;
    }

    @Override
    public void reclaimForRenderer() {
        provider.invoke("glFlush", NO_ARGUMENTS);
    }

    @Override
    public void finishProviderWork() {
        provider.invoke("glFinish", NO_ARGUMENTS);
    }
}
