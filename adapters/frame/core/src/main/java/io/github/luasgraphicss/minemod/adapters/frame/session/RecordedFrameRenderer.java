package io.github.luasgraphicss.minemod.adapters.frame.session;

import io.github.luasgraphicss.minemod.adapters.presentation.renderer.WindowRenderer;
import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;

public final class RecordedFrameRenderer implements WindowRenderer {
    private final ClientRenderer renderer;
    private final RendererScene scene;
    private final GameFrame frame;

    private RecordedFrameRenderer(ClientRenderer renderer) {
        this.renderer = renderer;
        this.scene = new RendererScene(renderer);
        this.frame = new GameFrame(scene);
    }

    public static WindowRenderer presenting(ClientRenderer renderer) {
        RecordedFrameRenderer recorded = new RecordedFrameRenderer(renderer);
        GameFrames.open(recorded.frame);
        return recorded;
    }

    @Override
    public void presentFrame() {
        scene.present();
    }

    @Override
    public void pace(PresentPacing pacing) {
        renderer.pace(pacing);
    }

    @Override
    public void close() {
        GameFrames.close(frame);
        renderer.close();
    }
}
