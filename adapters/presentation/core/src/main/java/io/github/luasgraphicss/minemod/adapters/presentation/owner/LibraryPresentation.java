package io.github.luasgraphicss.minemod.adapters.presentation.owner;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class LibraryPresentation implements PresentationOwner {
    @Override
    public boolean keepsOpenGlContext() {
        return true;
    }

    @Override
    public long openWindow(WindowRequest request, LongSupplier library) {
        return library.getAsLong();
    }

    @Override
    public void attachContext(long window, Runnable library) {
        library.run();
    }

    @Override
    public Object resolveCapabilities(Supplier<Object> library) {
        return library.get();
    }

    @Override
    public void presentFrame(long window, Runnable library) {
        library.run();
    }

    @Override
    public void pace(int swapInterval, Runnable library) {
        library.run();
    }

    @Override
    public void closeWindow(long window, Runnable library) {
        library.run();
    }

    @Override
    public String toString() {
        return "the windowing library of the game";
    }
}
