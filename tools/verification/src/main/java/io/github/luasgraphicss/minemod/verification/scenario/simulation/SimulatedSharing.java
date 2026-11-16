package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.interop.handoff.SharingBackend;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentOwner;

public final class SimulatedSharing implements SharingBackend {
    private final Timeline timeline;
    private AttachmentOwner owner = AttachmentOwner.RENDERER;

    public SimulatedSharing(Timeline timeline) {
        this.timeline = timeline;
    }

    @Override
    public void submitRendererWorkAndSignal() {
        timeline.add(Timeline.Source.HANDOFF, "renderer submits and signals");
    }

    @Override
    public void waitInProvider() {
        owner = AttachmentOwner.PROVIDER;
        timeline.add(Timeline.Source.HANDOFF, "provider waits");
    }

    @Override
    public void signalAndFlushProvider() {
        timeline.add(Timeline.Source.HANDOFF, "provider signals and flushes");
    }

    @Override
    public void waitAndReacquireInRenderer() {
        owner = AttachmentOwner.RENDERER;
        timeline.add(Timeline.Source.HANDOFF, "renderer waits and reacquires");
    }

    @Override
    public void finishProvider() {
        timeline.add(Timeline.Source.HANDOFF, "provider finishes");
    }

    public AttachmentOwner owner() {
        return owner;
    }
}
