package io.github.luasgraphicss.minemod.client.session;

import io.github.luasgraphicss.minemod.bridge.NativeSceneSession;
import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import io.github.luasgraphicss.minemod.bridge.PresentationState;
import io.github.luasgraphicss.minemod.client.presentation.PresentationTarget;

final class SessionPresentation implements PresentationTarget {
    private final NativeSceneSession session;

    SessionPresentation(NativeSceneSession session) {
        this.session = session;
    }

    @Override
    public void resize(int width, int height) {
        session.resize(width, height);
    }

    @Override
    public void setPacing(PresentPacing pacing) {
        session.setPacing(pacing);
    }

    @Override
    public PresentationState presentationState() {
        return session.presentationState();
    }
}
