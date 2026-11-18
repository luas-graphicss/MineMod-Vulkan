package io.github.luasgraphicss.minemod.adapters.frame.support;

import io.github.luasgraphicss.minemod.adapters.frame.draw.CapturedDraw;
import io.github.luasgraphicss.minemod.adapters.frame.session.FrameScene;
import io.github.luasgraphicss.minemod.client.frame.FramePixels;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.textures.TexturePublisher;
import io.github.luasgraphicss.minemod.contract.handle.HandleAllocator;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RecordingScene implements FrameScene {
    private static final int MAX_TEXTURES = 8;

    private final TextureObjects textures = new TextureObjects(new TexturePublisher(
            new HandleAllocator(ResourceKind.TEXTURE, TexturePublisher.handlesFor(MAX_TEXTURES)), MAX_TEXTURES));
    private final List<CapturedDraw> draws = new ArrayList<>();
    private final List<String> clears = new ArrayList<>();
    private boolean refusesDraws;

    public List<CapturedDraw> draws() {
        return draws;
    }

    public List<String> clears() {
        return clears;
    }

    public void refuseDraws() {
        refusesDraws = true;
    }

    @Override
    public TextureObjects textures() {
        return textures;
    }

    @Override
    public boolean record(CapturedDraw draw) {
        if (refusesDraws) {
            return false;
        }
        draws.add(draw);
        return true;
    }

    @Override
    public void clearColor(float red, float green, float blue) {
        clears.add("color " + red + " " + green + " " + blue);
    }

    @Override
    public void clearDepth() {
        clears.add("depth");
    }

    @Override
    public Optional<FramePixels> lastFrame() {
        return Optional.empty();
    }
}
