package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.frame;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.draw.CapturedState;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.terrain.TerrainView;
import io.github.luasgraphicss.minemod.client.draw.frame.DrawRecorder;
import io.github.luasgraphicss.minemod.client.draw.frame.GameDraw;
import io.github.luasgraphicss.minemod.client.draw.texture.SceneTextures;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import io.github.luasgraphicss.minemod.codec.scene.FrameBlur;
import io.github.luasgraphicss.minemod.codec.scene.TerrainState;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class GameFrame {
    private static final long STREAM_BYTES = 8L * 1024 * 1024 - 64 * 1024;
    private static final long STREAM_RECORDS = 32L * 1024 - 16;
    private static final int COLOR_BUFFER = 0x4000;
    private static final int DEPTH_BUFFER = 0x100;
    private static final int FAINTEST_BLUR = 1;
    private static final int BLUR_REPEATS = 3;
    private static final float OPAQUE = 1.0f;
    private static final DrawRecorder RECORDER = new DrawRecorder(STREAM_BYTES, STREAM_RECORDS);
    private static final CapturedState STATE = new CapturedState();
    private static boolean drawsWorld;
    private static boolean leavesWorldOut;
    private static long leftOutDraws;
    private static long leftOutWorlds;

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

    public static void blur(int radius) {
        if (radius >= FAINTEST_BLUR) {
            ModRendering.beforeNativeWork();
            RECORDER.blur(new FrameBlur(Math.min(radius, FrameBlur.MAX_RADIUS), BLUR_REPEATS));
            STATE.blend(false);
        }
    }

    public static void enterWorld(Vec3 camera, Matrix4f frustum, Matrix4f projection) {
        if (!isRecording()) {
            return;
        }
        Optional<TerrainState> terrain = terrainSeenFrom(camera, frustum, projection);
        if (terrain.isPresent() && !RECORDER.isPartlyWritten()) {
            RECORDER.enterWorld();
            RECORDER.terrain(terrain.get());
            drawsWorld = true;
        } else {
            leavesWorldOut = true;
            leftOutWorlds++;
        }
    }

    public static void seeTerrain(Vec3 camera, Matrix4f frustum, Matrix4f projection) {
        if (drawsWorld) {
            terrainSeenFrom(camera, frustum, projection).ifPresent(RECORDER::terrain);
        }
    }

    public static boolean drawsWorld() {
        return drawsWorld;
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

    public static long leftOutWorlds() {
        return leftOutWorlds;
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

    private static Optional<TerrainState> terrainSeenFrom(Vec3 camera, Matrix4f frustum, Matrix4f projection) {
        return TerrainView.seenFrom(
                camera, frustum, projection, textures(), ClientRendering.renderer().lightmap());
    }
}
