package io.github.luasgraphicss.minemod.compatibility.interop.handoff;

import java.util.ArrayList;
import java.util.List;

final class RecordingSharingBackend implements SharingBackend {
    static final String RENDERER_SIGNAL = "renderer submits and signals";
    static final String PROVIDER_WAIT = "provider waits";
    static final String PROVIDER_SIGNAL = "provider signals and flushes";
    static final String RENDERER_WAIT = "renderer waits and reacquires";
    static final String PROVIDER_FINISH = "provider finishes";

    private final List<String> steps = new ArrayList<>();
    private int failingProviderWaits;

    @Override
    public void submitRendererWorkAndSignal() {
        steps.add(RENDERER_SIGNAL);
    }

    @Override
    public void waitInProvider() {
        if (failingProviderWaits > 0) {
            failingProviderWaits--;
            throw new IllegalStateException("the provider context was lost");
        }
        steps.add(PROVIDER_WAIT);
    }

    @Override
    public void signalAndFlushProvider() {
        steps.add(PROVIDER_SIGNAL);
    }

    @Override
    public void waitAndReacquireInRenderer() {
        steps.add(RENDERER_WAIT);
    }

    @Override
    public void finishProvider() {
        steps.add(PROVIDER_FINISH);
    }

    void failNextProviderWait() {
        failingProviderWaits = 1;
    }

    List<String> steps() {
        return steps;
    }
}
