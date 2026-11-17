package io.github.luasgraphicss.minemod.adapters.presentation.fixtures;

import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class RecordingOwner implements PresentationOwner {
    public static final long WINDOW = 0x51L;
    private final List<String> steps = new ArrayList<>();
    private final boolean keepsOpenGlContext;
    private final Object capabilities;

    public RecordingOwner(boolean keepsOpenGlContext, Object capabilities) {
        this.keepsOpenGlContext = keepsOpenGlContext;
        this.capabilities = capabilities;
    }

    public List<String> steps() {
        return steps;
    }

    @Override
    public boolean keepsOpenGlContext() {
        return keepsOpenGlContext;
    }

    @Override
    public long openWindow(WindowRequest request, LongSupplier library) {
        steps.add("open " + request);
        return request.isDisplay() ? WindowRequest.DISPLAY_WINDOW : WINDOW;
    }

    @Override
    public void attachContext(long window, Runnable library) {
        steps.add("attach " + window);
    }

    @Override
    public Object resolveCapabilities(Supplier<Object> library) {
        steps.add("capabilities");
        return capabilities;
    }

    @Override
    public void presentFrame(long window, Runnable library) {
        steps.add("present " + window);
    }

    @Override
    public void pace(int swapInterval, Runnable library) {
        steps.add("pace " + swapInterval);
    }

    @Override
    public void closeWindow(long window, Runnable library) {
        steps.add("close " + window);
    }

    @Override
    public String toString() {
        return "a recording owner";
    }
}
