package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import io.github.luasgraphicss.minemod.compatibility.interop.handoff.SharingBackend;
import java.util.Objects;

public final class SplitSharingBackend implements SharingBackend {
    private final RendererHandoff renderer;
    private final ProviderHandoff provider;

    public SplitSharingBackend(RendererHandoff renderer, ProviderHandoff provider) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public void submitRendererWorkAndSignal() {
        renderer.submitWorkAndSignal();
    }

    @Override
    public void waitInProvider() {
        provider.waitForRenderer();
    }

    @Override
    public void signalAndFlushProvider() {
        provider.signalAndFlush();
    }

    @Override
    public void waitAndReacquireInRenderer() {
        renderer.waitAndReacquire();
    }

    @Override
    public void finishProvider() {
        provider.finish();
    }
}
