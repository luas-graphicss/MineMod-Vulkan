package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.frame;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.draw.CapturedState;
import io.github.luasgraphicss.minemod.client.draw.frame.DrawRecorder;
import io.github.luasgraphicss.minemod.client.draw.texture.SceneTextures;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import net.minecraft.client.Minecraft;

public final class GameFrame {
    private static final long STREAM_BYTES = 8L * 1024 * 1024 - 64 * 1024;
    private static final long STREAM_RECORDS = 32L * 1024 - 16;
    private static final int COLOR_BUFFER = 0x4000;
    private static final float OPAQUE = 1.0f;
    private static final DrawRecorder RECORDER = new DrawRecorder(STREAM_BYTES, STREAM_RECORDS);
    private static final CapturedState STATE = new CapturedState();

    private static volatile SceneTextures textures = SceneTextures.NONE;
    private static boolean drawsWorld;
    private static long leftOutDraws;

    private GameFrame() {}

    public static boolean isRecording() {
        return ClientRendering.running().isPresent();
    }

    public static CapturedState state() {
        return STATE;
    }

    public static DrawRecorder recorder() {
        return RECORDER;
    }

    public static SceneTextures textures() {
        return textures;
    }

    public static void findTexturesIn(SceneTextures published) {
        textures = published;
    }

    public static boolean drawsIntoTheWindow() {
        Object target = STATE.target();
        return target != null && target == Minecraft.getInstance().getMainRenderTarget();
    }

    public static void cleared(int buffers) {
        if ((buffers & COLOR_BUFFER) != 0 && drawsIntoTheWindow()) {
            float[] color = STATE.clearColor();
            RECORDER.clearColor(color[0], color[1], color[2], OPAQUE);
        }
    }

    public static void enterWorld() {
        if (isRecording()) {
            RECORDER.enterWorld();
            drawsWorld = true;
        }
    }

    public static boolean drawsWorld() {
        return drawsWorld;
    }

    public static void enter(BatchSlot stage) {
        if (isRecording() && (drawsWorld || stage == BatchSlot.INTERFACE)) {
            RECORDER.enter(stage);
        }
    }

    public static void leaveOut() {
        leftOutDraws++;
    }

    public static long leftOutDraws() {
        return leftOutDraws;
    }

    public static void present(ClientRenderer renderer) {
        renderer.presentFrame(RECORDER);
        RECORDER.beginFrame();
        drawsWorld = false;
    }
}
