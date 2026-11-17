package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class BlockingHandoff implements AttachmentHandoff {
    private static final long[] NO_ARGUMENTS = new long[0];

    private final ProviderCalls provider;

    public BlockingHandoff(ProviderCalls provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public void releaseToProvider() {
    }

    @Override
    public void reclaimForRenderer() {
        provider.invoke("glFinish", NO_ARGUMENTS);
    }

    @Override
    public void finishProviderWork() {
        provider.invoke("glFinish", NO_ARGUMENTS);
    }
}
