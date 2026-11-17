package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import io.github.luasgraphicss.minemod.compatibility.interop.handoff.SharingBackend;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class BlockingSharingBackend implements SharingBackend {
    private static final long[] NO_ARGUMENTS = new long[0];

    private final RendererHandoff renderer;
    private final ProviderCalls provider;

    public BlockingSharingBackend(RendererHandoff renderer, ProviderCalls provider) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public void submitRendererWorkAndSignal() {
        renderer.submitWorkAndSignal();
    }

    @Override
    public void waitInProvider() {
    }

    @Override
    public void signalAndFlushProvider() {
        provider.invoke("glFinish", NO_ARGUMENTS);
    }

    @Override
    public void waitAndReacquireInRenderer() {
        renderer.waitAndReacquire();
    }

    @Override
    public void finishProvider() {
        provider.invoke("glFinish", NO_ARGUMENTS);
    }
}
