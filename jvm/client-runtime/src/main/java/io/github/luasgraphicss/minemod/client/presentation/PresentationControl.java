package io.github.luasgraphicss.minemod.client.presentation;

import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import io.github.luasgraphicss.minemod.bridge.PresentationState;
import io.github.luasgraphicss.minemod.client.failure.RendererFailure;
import io.github.luasgraphicss.minemod.client.window.FramebufferSize;
import io.github.luasgraphicss.minemod.client.window.WindowSurface;

public final class PresentationControl {
    private final PresentationTarget target;
    private final WindowSurface window;
    private FramebufferSize appliedSize;
    private FramebufferSize frameSize;
    private PresentPacing appliedPacing = PresentPacing.VSYNC;
    private PresentPacing requestedPacing = PresentPacing.VSYNC;

    public PresentationControl(PresentationTarget target, WindowSurface window, FramebufferSize openedSize) {
        this.target = target;
        this.window = window;
        this.appliedSize = openedSize;
        this.frameSize = openedSize;
    }

    public FramebufferSize frameSize() {
        return frameSize;
    }

    public void pace(PresentPacing pacing) {
        requestedPacing = pacing;
    }

    public boolean beginFrame() {
        FramebufferSize size = window.framebufferSize();
        if (!size.equals(appliedSize)) {
            target.resize(size.width(), size.height());
            appliedSize = size;
        }
        if (size.hasArea()) {
            frameSize = size;
        }
        if (requestedPacing != appliedPacing) {
            target.setPacing(requestedPacing);
            appliedPacing = requestedPacing;
        }
        return size.hasArea();
    }

    public void endFrame() {
        if (target.presentationState() == PresentationState.DEVICE_LOST) {
            throw new RendererFailure("the graphics device was lost while the frame was presented");
        }
    }
}
