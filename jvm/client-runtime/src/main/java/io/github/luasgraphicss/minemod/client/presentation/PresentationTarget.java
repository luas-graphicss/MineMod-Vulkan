package io.github.luasgraphicss.minemod.client.presentation;

import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import io.github.luasgraphicss.minemod.bridge.PresentationState;

public interface PresentationTarget {
    void resize(int width, int height);

    void setPacing(PresentPacing pacing);

    PresentationState presentationState();
}
