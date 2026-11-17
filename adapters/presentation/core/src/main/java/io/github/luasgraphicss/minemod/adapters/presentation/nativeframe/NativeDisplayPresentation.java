package io.github.luasgraphicss.minemod.adapters.presentation.nativeframe;

import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class NativeDisplayPresentation implements PresentationOwner {
    private static final int UNPACED = 0;
    private final FramePresenter presenter;
    private final StandInWindow window;
    private boolean isOpen;

    public NativeDisplayPresentation(FramePresenter presenter, StandInWindow window) {
        this.presenter = Objects.requireNonNull(presenter, "presenter");
        this.window = Objects.requireNonNull(window, "window");
    }

    @Override
    public boolean keepsOpenGlContext() {
        return false;
    }

    @Override
    public long openWindow(WindowRequest request, LongSupplier library) {
        if (!request.isDisplay()) {
            throw new IllegalArgumentException("the stand-in for the LWJGL 2 display cannot open " + request);
        }
        window.open();
        try {
            presenter.start(WindowRequest.DISPLAY_WINDOW);
        } catch (RuntimeException failure) {
            window.close();
            throw failure;
        }
        isOpen = true;
        return WindowRequest.DISPLAY_WINDOW;
    }

    @Override
    public void attachContext(long attached, Runnable library) {
        throw new UnsupportedOperationException("the stand-in for the LWJGL 2 display has no OpenGL context to attach");
    }

    @Override
    public Object resolveCapabilities(Supplier<Object> library) {
        throw new UnsupportedOperationException("the stand-in for the LWJGL 2 display has no OpenGL capabilities");
    }

    @Override
    public void presentFrame(long presented, Runnable library) {
        presenter.present();
    }

    @Override
    public void pace(int swapInterval, Runnable library) {
        presenter.pace(swapInterval != UNPACED);
    }

    @Override
    public void closeWindow(long closed, Runnable library) {
        if (!isOpen) {
            return;
        }
        isOpen = false;
        try {
            presenter.stop();
        } finally {
            window.close();
        }
    }

    @Override
    public String toString() {
        return "the native renderer on a stand-in for the LWJGL 2 display";
    }
}
