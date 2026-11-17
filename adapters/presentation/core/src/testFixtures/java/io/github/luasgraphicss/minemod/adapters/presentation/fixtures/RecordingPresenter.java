package io.github.luasgraphicss.minemod.adapters.presentation.fixtures;

import io.github.luasgraphicss.minemod.adapters.presentation.nativeframe.FramePresenter;
import java.util.ArrayList;
import java.util.List;

public final class RecordingPresenter implements FramePresenter {
    public static final Object CAPABILITIES = new Object();
    private final List<String> steps = new ArrayList<>();

    public List<String> steps() {
        return steps;
    }

    @Override
    public void start(long window) {
        steps.add("start " + window);
    }

    @Override
    public Object capabilities() {
        steps.add("capabilities");
        return CAPABILITIES;
    }

    @Override
    public void present() {
        steps.add("present");
    }

    @Override
    public void pace(boolean synchronised) {
        steps.add(synchronised ? "pace vsync" : "pace immediate");
    }

    @Override
    public void stop() {
        steps.add("stop");
    }
}
