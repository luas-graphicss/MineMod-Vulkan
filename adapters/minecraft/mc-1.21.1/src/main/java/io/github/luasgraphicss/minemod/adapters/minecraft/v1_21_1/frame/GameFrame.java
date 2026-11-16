package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.frame;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.draw.CapturedState;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.terrain.TerrainView;
import io.github.luasgraphicss.minemod.client.draw.frame.DrawRecorder;
import io.github.luasgraphicss.minemod.client.draw.texture.SceneTextures;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import io.github.luasgraphicss.minemod.codec.scene.TerrainState;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class GameFrame {
    private static final long STREAM_BYTES = 8L * 1024 * 1024 - 64 * 1024;
    private static final long STREAM_RECORDS = 32L * 1024 - 16;
    private static final int COLOR_BUFFER = 0x4000;
    private static final float OPAQUE = 1.0f;
    private static final DrawRecorder RECORDER = new DrawRecorder(STREAM_BYTES, STREAM_RECORDS);
    private static final CapturedState STATE = new CapturedState();
    private static volatile SceneTextures textures = SceneTextures.NONE;
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

    public static void enterWorld(Vec3 camera, Matrix4f frustum, Matrix4f projection) {
        if (!isRecording()) {
            return;
        }
        Optional<TerrainState> terrain = terrainSeenFrom(camera, frustum, projection);
        if (terrain.isPresent()) {
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

    public static void present(ClientRenderer renderer) {
        renderer.presentFrame(RECORDER);
        RECORDER.beginFrame();
        drawsWorld = false;
        leavesWorldOut = false;
    }

    private static Optional<TerrainState> terrainSeenFrom(Vec3 camera, Matrix4f frustum, Matrix4f projection) {
        return TerrainView.seenFrom(
                camera, frustum, projection, textures, ClientRendering.renderer().lightmap());
    }
}
