package io.github.luasgraphicss.minemod.adapters.frame.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.frame.calls.ClearCalls;
import io.github.luasgraphicss.minemod.adapters.frame.calls.DisplayListCalls;
import io.github.luasgraphicss.minemod.adapters.frame.calls.ImmediateCalls;
import io.github.luasgraphicss.minemod.adapters.frame.calls.MatrixCalls;
import io.github.luasgraphicss.minemod.adapters.frame.calls.SurfaceCalls;
import io.github.luasgraphicss.minemod.adapters.frame.calls.SwitchCalls;
import io.github.luasgraphicss.minemod.adapters.frame.calls.TextureCalls;
import io.github.luasgraphicss.minemod.adapters.frame.draw.CapturedDraw;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.Matrices;
import io.github.luasgraphicss.minemod.adapters.frame.support.RecordingScene;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameFrameTest {
    private static final int PROJECTION = 0x1701;
    private static final int MODEL_VIEW = 0x1700;
    private static final int TRIANGLES = 4;
    private static final int COMPILE = 0x1300;
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int COLOR_BUFFER = 0x4000;
    private static final int DEPTH_BUFFER = 0x100;
    private static final int WINDOW_WIDTH = 854;
    private static final int WINDOW_HEIGHT = 480;
    private static final float TOLERANCE = 1.0e-6f;

    private final RecordingScene scene = new RecordingScene();
    private final GameFrame frame = new GameFrame(scene);

    @BeforeEach
    void presentTheFrame() {
        GameFrames.open(frame);
    }

    @AfterEach
    void stopPresenting() {
        GameFrames.close(frame);
    }

    @Test
    void recordsAnImmediateTriangleUnderTheMatricesTheGameSelected() {
        MatrixCalls.glMatrixMode(PROJECTION);
        MatrixCalls.glOrtho(0.0, WINDOW_WIDTH, WINDOW_HEIGHT, 0.0, 1000.0, 3000.0);
        MatrixCalls.glMatrixMode(MODEL_VIEW);
        MatrixCalls.glTranslatef(1.0f, 2.0f, 3.0f);

        drawTriangle();

        CapturedDraw draw = onlyDraw();
        assertEquals(PrimitiveMode.TRIANGLES, draw.mode());
        assertEquals(3, draw.geometry().vertexCount());
        assertEquals(1.0f, draw.modelView()[Matrices.at(3, 0)], TOLERANCE);
        assertEquals(3.0f, draw.modelView()[Matrices.at(3, 2)], TOLERANCE);
        assertEquals(2.0f / WINDOW_WIDTH, draw.projection()[Matrices.at(0, 0)], TOLERANCE);
        assertFalse(draw.shading().contains(Shading.TEXTURE));
    }

    @Test
    void restoresTheMatrixTheGamePushed() {
        MatrixCalls.glPushMatrix();
        MatrixCalls.glScalef(4.0f, 4.0f, 4.0f);
        MatrixCalls.glPopMatrix();

        drawTriangle();

        assertTrue(Matrices.isIdentity(onlyDraw().modelView()));
    }

    @Test
    void samplesTheTextureBoundWhenTheVerticesCarryCoordinates() {
        int atlas = TextureCalls.glGenTextures();
        TextureCalls.glBindTexture(TEXTURE_2D, atlas);
        SwitchCalls.glEnable(TEXTURE_2D);

        ImmediateCalls.glBegin(TRIANGLES);
        ImmediateCalls.glTexCoord2f(0.0f, 0.0f);
        ImmediateCalls.glVertex3f(0.0f, 0.0f, 0.0f);
        ImmediateCalls.glTexCoord2f(1.0f, 0.0f);
        ImmediateCalls.glVertex3f(1.0f, 0.0f, 0.0f);
        ImmediateCalls.glTexCoord2f(0.0f, 1.0f);
        ImmediateCalls.glVertex3f(0.0f, 1.0f, 0.0f);
        ImmediateCalls.glEnd();

        CapturedDraw draw = onlyDraw();
        assertTrue(draw.shading().contains(Shading.TEXTURE));
        assertEquals(atlas, draw.sampledTexture());
    }

    @Test
    void tintsVerticesWithoutColorsWithTheCurrentColor() {
        SurfaceCalls.glColor4f(0.25f, 0.5f, 0.75f, 1.0f);

        drawTriangle();

        float[] color = onlyDraw().color().orElseThrow(AssertionError::new);
        assertTrue(Arrays.equals(new float[] {0.25f, 0.5f, 0.75f, 1.0f}, color), Arrays.toString(color));
    }

    @Test
    void replaysACompiledListEachTimeTheGameCallsIt() {
        int list = DisplayListCalls.glGenLists(1);
        DisplayListCalls.glNewList(list, COMPILE);
        MatrixCalls.glTranslatef(5.0f, 0.0f, 0.0f);
        drawTriangle();
        DisplayListCalls.glEndList();
        assertTrue(scene.draws().isEmpty());

        DisplayListCalls.glCallList(list);
        DisplayListCalls.glCallList(list);

        assertEquals(2, scene.draws().size());
        assertEquals(5.0f, scene.draws().get(0).modelView()[Matrices.at(3, 0)], TOLERANCE);
        assertEquals(10.0f, scene.draws().get(1).modelView()[Matrices.at(3, 0)], TOLERANCE);
    }

    @Test
    void forgetsAListTheGameDeleted() {
        int list = DisplayListCalls.glGenLists(1);
        DisplayListCalls.glNewList(list, COMPILE);
        drawTriangle();
        DisplayListCalls.glEndList();

        DisplayListCalls.glDeleteLists(list, 1);
        DisplayListCalls.glCallList(list);

        assertTrue(scene.draws().isEmpty());
    }

    @Test
    void countsTheDrawsTheSceneCannotTake() {
        scene.refuseDraws();

        drawTriangle();

        assertEquals(1L, frame.leftOutDraws());
    }

    @Test
    void clearsWithTheColorTheGameChose() {
        ClearCalls.glClearColor(0.5f, 0.25f, 0.125f, 1.0f);

        ClearCalls.glClear(COLOR_BUFFER | DEPTH_BUFFER);

        assertEquals(Arrays.asList("color 0.5 0.25 0.125", "depth"), scene.clears());
    }

    @Test
    void refusesOpenGlCallsWhileNoWindowPresentsTheFrame() {
        GameFrames.close(frame);

        assertThrows(IllegalStateException.class, MatrixCalls::glLoadIdentity);
    }

    private CapturedDraw onlyDraw() {
        assertEquals(1, scene.draws().size());
        return scene.draws().get(0);
    }

    private static void drawTriangle() {
        ImmediateCalls.glBegin(TRIANGLES);
        ImmediateCalls.glVertex3f(0.0f, 0.0f, 0.0f);
        ImmediateCalls.glVertex3f(1.0f, 0.0f, 0.0f);
        ImmediateCalls.glVertex3f(0.0f, 1.0f, 0.0f);
        ImmediateCalls.glEnd();
    }
}
