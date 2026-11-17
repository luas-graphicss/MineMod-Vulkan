package io.github.luasgraphicss.minemod.adapters.presentation.owner;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

public interface PresentationOwner {
    boolean keepsOpenGlContext();

    long openWindow(WindowRequest request, LongSupplier library);

    void attachContext(long window, Runnable library);

    Object resolveCapabilities(Supplier<Object> library);

    void presentFrame(long window, Runnable library);

    void pace(int swapInterval, Runnable library);

    void closeWindow(long window, Runnable library);
}
