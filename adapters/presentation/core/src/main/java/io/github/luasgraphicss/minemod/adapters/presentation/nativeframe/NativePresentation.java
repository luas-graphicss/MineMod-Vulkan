package io.github.luasgraphicss.minemod.adapters.presentation.nativeframe;

import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class NativePresentation implements PresentationOwner {
    private static final long NO_WINDOW = 0L;
    private static final int UNPACED = 0;
    private final FramePresenter presenter;
    private long window = NO_WINDOW;
    private boolean started;

    public NativePresentation(FramePresenter presenter) {
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    @Override
    public boolean keepsOpenGlContext() {
        return false;
    }

    @Override
    public long openWindow(WindowRequest request, LongSupplier library) {
        if (request.isDisplay()) {
            throw new UnsupportedOperationException(
                    "the LWJGL 2 display always creates an OpenGL context; the native renderer cannot present it");
        }
        window = library.getAsLong();
        return window;
    }

    @Override
    public void attachContext(long attached, Runnable library) {
        if (attached == NO_WINDOW || attached != window) {
            throw new IllegalArgumentException(
                    "the native renderer presents window " + window + ", not window " + attached);
        }
        presenter.start(attached);
        started = true;
    }

    @Override
    public Object resolveCapabilities(Supplier<Object> library) {
        return presenter.capabilities();
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
        try {
            if (started) {
                started = false;
                presenter.stop();
            }
        } finally {
            window = NO_WINDOW;
            library.run();
        }
    }

    @Override
    public String toString() {
        return "the native renderer";
    }
}
