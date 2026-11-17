package io.github.luasgraphicss.minemod.adapters.presentation.lifecycle;

import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class GuardedPresentation implements PresentationOwner {
    private static final long NO_WINDOW = 0L;
    private final PresentationOwner owner;
    private PresentationPhase phase = PresentationPhase.UNOPENED;
    private long framesPresented;

    public GuardedPresentation(PresentationOwner owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public synchronized PresentationPhase phase() {
        return phase;
    }

    public synchronized long framesPresented() {
        return framesPresented;
    }

    @Override
    public boolean keepsOpenGlContext() {
        return owner.keepsOpenGlContext();
    }

    @Override
    public synchronized long openWindow(WindowRequest request, LongSupplier library) {
        require("open " + request, PresentationPhase.UNOPENED, PresentationPhase.CLOSED);
        long window = owner.openWindow(request, library);
        if (request.isDisplay()) {
            phase = PresentationPhase.READY;
        } else if (window != NO_WINDOW) {
            phase = PresentationPhase.OPENED;
        }
        return window;
    }

    @Override
    public synchronized void attachContext(long window, Runnable library) {
        require("attach the context", PresentationPhase.OPENED);
        owner.attachContext(window, library);
        phase = PresentationPhase.ATTACHED;
    }

    @Override
    public synchronized Object resolveCapabilities(Supplier<Object> library) {
        require("resolve the OpenGL capabilities", PresentationPhase.ATTACHED);
        Object capabilities = owner.resolveCapabilities(library);
        phase = PresentationPhase.READY;
        return capabilities;
    }

    @Override
    public synchronized void presentFrame(long window, Runnable library) {
        require("present a frame", PresentationPhase.READY);
        owner.presentFrame(window, library);
        framesPresented++;
    }

    @Override
    public synchronized void pace(int swapInterval, Runnable library) {
        require("pace the presentation", PresentationPhase.READY);
        owner.pace(swapInterval, library);
    }

    @Override
    public synchronized void closeWindow(long window, Runnable library) {
        require("close the window", PresentationPhase.OPENED, PresentationPhase.ATTACHED, PresentationPhase.READY);
        try {
            owner.closeWindow(window, library);
        } finally {
            phase = PresentationPhase.CLOSED;
        }
    }

    @Override
    public String toString() {
        return owner.toString();
    }

    private void require(String step, PresentationPhase... allowed) {
        for (PresentationPhase candidate : allowed) {
            if (phase == candidate) {
                return;
            }
        }
        throw new PresentationOrderException(step, phase);
    }
}
