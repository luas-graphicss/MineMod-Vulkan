package io.github.luasgraphicss.minemod.adapters.minecraft.frame;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.CapturedState;
import io.github.luasgraphicss.minemod.adapters.minecraft.terrain.TerrainView;
import io.github.luasgraphicss.minemod.client.draw.frame.DrawRecorder;
import io.github.luasgraphicss.minemod.client.draw.frame.GameDraw;
import io.github.luasgraphicss.minemod.client.draw.texture.SceneTextures;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import io.github.luasgraphicss.minemod.codec.scene.TerrainState;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class GameFrame {
    private static final long STREAM_BYTES = 8L * 1024 * 1024 - 64 * 1024;
    private static final long STREAM_RECORDS = 32L * 1024 - 16;
    private static final int COLOR_BUFFER = 0x4000;
    private static final int DEPTH_BUFFER = 0x100;
    private static final float OPAQUE = 1.0f;
    private static final DrawRecorder RECORDER = new DrawRecorder(STREAM_BYTES, STREAM_RECORDS);
    private static final CapturedState STATE = new CapturedState();

    private static boolean drawsWorld;
    private static boolean leavesWorldOut;
    private static long leftOutDraws;

    private GameFrame() {}

    public static boolean isRecording() {
        return ClientRendering.running().isPresent();
    }

    public static CapturedState state() {
        return STATE;
    }

    public static void record(GameDraw draw) {
        ModRendering.beforeNativeWork();
        RECORDER.draw(draw);
    }

    public static SceneTextures textures() {
        return ClientRendering.renderer().textures();
    }

    public static boolean drawsIntoTheWindow() {
        Object target = STATE.target();
        return target != null && target == Minecraft.getInstance().getMainRenderTarget();
    }

    public static void cleared(int buffers) {
        if (!drawsIntoTheWindow()) {
            return;
        }
        ModRendering.beforeNativeWork();
        if ((buffers & COLOR_BUFFER) != 0) {
            float[] color = STATE.clearColor();
            RECORDER.clearColor(color[0], color[1], color[2], OPAQUE);
        }
        if ((buffers & DEPTH_BUFFER) != 0) {
            RECORDER.clearDepth();
        }
    }

    public static void enterWorld(Vec3 camera) {
        if (!isRecording()) {
            return;
        }
        Optional<TerrainState> terrain = terrainSeenFrom(camera);
        if (terrain.isPresent() && !RECORDER.isPartlyWritten()) {
            RECORDER.enterWorld();
            RECORDER.terrain(terrain.get());
            drawsWorld = true;
        } else {
            leavesWorldOut = true;
        }
    }

    public static void seeTerrain(Vec3 camera) {
        if (drawsWorld) {
            terrainSeenFrom(camera).ifPresent(RECORDER::terrain);
        }
    }

    public static boolean leavesTheWorldOut() {
        return leavesWorldOut;
    }

    public static void enter(BatchSlot stage) {
        if (stage == BatchSlot.INTERFACE) {
            leavesWorldOut = false;
        }
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

    public static boolean submitPart(ClientRenderer renderer) {
        return renderer.submitPart(RECORDER);
    }

    public static void present(ClientRenderer renderer) {
        ModRendering.present();
        renderer.presentFrame(RECORDER);
        RECORDER.beginFrame();
        drawsWorld = false;
        leavesWorldOut = false;
    }

    private static Optional<TerrainState> terrainSeenFrom(Vec3 camera) {
        return TerrainView.seenFrom(camera, textures(), ClientRendering.renderer().lightmap());
    }
}
