package io.github.luasgraphicss.minemod.adapters.frame.session;

import io.github.luasgraphicss.minemod.adapters.frame.draw.CapturedDraw;
import io.github.luasgraphicss.minemod.client.draw.frame.DrawRecorder;
import io.github.luasgraphicss.minemod.client.draw.frame.GameDraw;
import io.github.luasgraphicss.minemod.client.frame.FramePixels;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import java.util.Optional;

final class RendererScene implements FrameScene {
    private static final long STREAM_BYTES = 8L * 1024 * 1024 - 64 * 1024;
    private static final long STREAM_RECORDS = 32L * 1024 - 16;
    private static final float OPAQUE = 1.0f;

    private final ClientRenderer renderer;
    private final DrawRecorder recorder = new DrawRecorder(STREAM_BYTES, STREAM_RECORDS);

    RendererScene(ClientRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public TextureObjects textures() {
        return renderer.openGl().textures();
    }

    @Override
    public boolean record(CapturedDraw draw) {
        Optional<GameDraw> scene = SceneDraws.of(draw, renderer.textures());
        if (!scene.isPresent()) {
            return false;
        }
        if (recorder.draw(scene.get())) {
            return true;
        }
        return renderer.submitPart(recorder) && recorder.draw(scene.get());
    }

    @Override
    public void clearColor(float red, float green, float blue) {
        recorder.clearColor(red, green, blue, OPAQUE);
    }

    @Override
    public void clearDepth() {
        recorder.clearDepth();
    }

    @Override
    public Optional<FramePixels> lastFrame() {
        return renderer.readFrame();
    }

    void present() {
        renderer.presentFrame(recorder);
        recorder.beginFrame();
    }
}
