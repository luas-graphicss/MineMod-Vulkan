package io.github.luasgraphicss.minemod.adapters.presentation.renderer;

import io.github.luasgraphicss.minemod.bridge.PresentPacing;

public interface WindowRenderer {
    void presentFrame();

    void pace(PresentPacing pacing);

    void close();
}
