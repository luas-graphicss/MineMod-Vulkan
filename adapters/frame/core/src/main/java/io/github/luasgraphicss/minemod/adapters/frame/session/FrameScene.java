package io.github.luasgraphicss.minemod.adapters.frame.session;

import io.github.luasgraphicss.minemod.adapters.frame.draw.CapturedDraw;
import io.github.luasgraphicss.minemod.client.frame.FramePixels;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import java.util.Optional;

public interface FrameScene {
    TextureObjects textures();

    boolean record(CapturedDraw draw);

    void clearColor(float red, float green, float blue);

    void clearDepth();

    Optional<FramePixels> lastFrame();
}
