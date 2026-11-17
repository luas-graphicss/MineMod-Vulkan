package io.github.luasgraphicss.minemod.adapters.presentation.renderer.session;

import io.github.luasgraphicss.minemod.adapters.presentation.renderer.WindowRenderer;
import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import io.github.luasgraphicss.minemod.client.frame.FrameContent;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;

final class ClearedFrameRenderer implements WindowRenderer {
    private final ClientRenderer renderer;
    private final FrameContent frame = new ClearedFrame();

    ClearedFrameRenderer(ClientRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void presentFrame() {
        renderer.presentFrame(frame);
    }

    @Override
    public void pace(PresentPacing pacing) {
        renderer.pace(pacing);
    }

    @Override
    public void close() {
        renderer.close();
    }
}
