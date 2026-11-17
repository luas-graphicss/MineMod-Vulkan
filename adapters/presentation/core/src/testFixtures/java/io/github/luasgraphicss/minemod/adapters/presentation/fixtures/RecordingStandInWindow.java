package io.github.luasgraphicss.minemod.adapters.presentation.fixtures;

import io.github.luasgraphicss.minemod.adapters.presentation.nativeframe.StandInWindow;
import java.util.List;

public final class RecordingStandInWindow implements StandInWindow {
    private final List<String> steps;

    public RecordingStandInWindow(List<String> steps) {
        this.steps = steps;
    }

    @Override
    public void open() {
        steps.add("open stand-in");
    }

    @Override
    public void close() {
        steps.add("close stand-in");
    }
}
