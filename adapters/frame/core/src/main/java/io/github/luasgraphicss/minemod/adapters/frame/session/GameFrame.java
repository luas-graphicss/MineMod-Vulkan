package io.github.luasgraphicss.minemod.adapters.frame.session;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.ArrayBuffers;
import io.github.luasgraphicss.minemod.adapters.frame.arrays.ClientArrays;
import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.adapters.frame.draw.CapturedDraw;
import io.github.luasgraphicss.minemod.adapters.frame.draw.DrawCapture;
import io.github.luasgraphicss.minemod.adapters.frame.immediate.ImmediateVertices;
import io.github.luasgraphicss.minemod.adapters.frame.lists.DisplayLists;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.TextureUnits;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import java.util.Optional;
import java.util.function.Consumer;

public final class GameFrame {
    private static final int COLOR_BUFFER = 0x4000;
    private static final int DEPTH_BUFFER = 0x100;
    private static final int VIEWPORT_VALUES = 4;

    private final FrameScene scene;
    private final FixedPipeline pipeline;
    private final ClientArrays arrays = new ClientArrays();
    private final ArrayBuffers buffers = new ArrayBuffers();
    private final ImmediateVertices immediate = new ImmediateVertices();
    private final DisplayLists<Consumer<GameFrame>> lists = new DisplayLists<>();
    private final int[] viewport = new int[VIEWPORT_VALUES];
    private long leftOutDraws;

    public GameFrame(FrameScene scene) {
        this.scene = scene;
        this.pipeline = new FixedPipeline(() -> scene.textures().activeUnit());
    }

    public FrameScene scene() {
        return scene;
    }

    public TextureObjects textures() {
        return scene.textures();
    }

    public FixedPipeline pipeline() {
        return pipeline;
    }

    public ClientArrays arrays() {
        return arrays;
    }

    public ArrayBuffers buffers() {
        return buffers;
    }

    public ImmediateVertices immediate() {
        return immediate;
    }

    public DisplayLists<Consumer<GameFrame>> lists() {
        return lists;
    }

    public void run(Consumer<GameFrame> command) {
        if (lists.isCompiling()) {
            lists.record(command);
        } else {
            command.accept(this);
        }
    }

    public void call(int list) {
        for (Consumer<GameFrame> command : lists.commandsOf(list)) {
            command.accept(this);
        }
    }

    public void draw(Geometry geometry) {
        if (lists.isCompiling()) {
            Geometry kept = geometry.copy();
            lists.record(frame -> frame.record(kept));
        } else {
            record(geometry);
        }
    }

    public void leaveOut() {
        leftOutDraws++;
    }

    public long leftOutDraws() {
        return leftOutDraws;
    }

    public void clear(int clearedBuffers) {
        if ((clearedBuffers & COLOR_BUFFER) != 0) {
            float[] color = pipeline.surface().clearColor();
            scene.clearColor(color[0], color[1], color[2]);
        }
        if ((clearedBuffers & DEPTH_BUFFER) != 0) {
            scene.clearDepth();
        }
    }

    public void viewport(int left, int bottom, int width, int height) {
        viewport[0] = left;
        viewport[1] = bottom;
        viewport[2] = width;
        viewport[3] = height;
    }

    public int[] viewport() {
        return viewport.clone();
    }

    private void record(Geometry geometry) {
        Optional<CapturedDraw> draw = DrawCapture.of(pipeline, geometry, this::textureBoundOn);
        if (!draw.isPresent() || !scene.record(draw.get())) {
            leftOutDraws++;
        }
    }

    private int textureBoundOn(int unit) {
        TextureObjects textures = scene.textures();
        int active = textures.activeUnit();
        textures.activate(TextureUnits.FIRST_NAME + unit);
        int bound = textures.bound();
        textures.activate(active);
        return bound;
    }
}
