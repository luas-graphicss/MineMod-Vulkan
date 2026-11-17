package io.github.luasgraphicss.minemod.compatibility.lwjgl2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.DispatchInstaller;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.runtime.CompatibilitySession;
import io.github.luasgraphicss.minemod.compatibility.runtime.SessionContext;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlWords;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
@EnabledOnJre(JRE.JAVA_8)
class Lwjgl2OnZinkTest {
    private static final int WIDTH = ProviderTestEnvironment.SCENE_WIDTH;
    private static final int HEIGHT = ProviderTestEnvironment.SCENE_HEIGHT;
    private static final int BLACK = 0xFF000000;
    private static final int RED = 0xFF0000FF;
    private static final int GREEN = 0xFF00FF00;
    private static final int CHANNELS = 4;

    @TempDir
    Path providerDirectory;

    private final Lwjgl2Dispatch binding = new Lwjgl2Dispatch();
    private CompatibilitySession session;
    private SessionContext context;
    private CompatibilityLayer layer;

    @BeforeEach
    void installTheDispatchBeforeLwjglLoadsAnOpenGlLibrary() throws Exception {
        session = ProviderTestEnvironment.open(
                ProviderTestEnvironment.stage(providerDirectory), ContextProfile.COMPATIBILITY);
        session.install(new DispatchInstaller(binding, binding), BindingGeneration.LWJGL2);
        context = session.createContext(ProviderTestEnvironment.standalonePlan());
        layer = context.layer();
        GLContext.useContext(context);
        layer.beginFrame();
        layer.enterCallback();
        GL11.glViewport(0, 0, WIDTH, HEIGHT);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    @AfterEach
    void removeTheDispatch() {
        binding.uninstall();
        context.close();
        session.close();
    }

    @Test
    void lwjglSeesTheLegacyVersionsAndOnlyTheExtensionsTheLayerExposes() {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        assertTrue(capabilities.OpenGL11);
        assertTrue(capabilities.OpenGL15);
        assertTrue(capabilities.OpenGL20);
        assertEquals(
                session.exposedExtensions().contains("GL_ARB_sampler_objects"), capabilities.GL_ARB_sampler_objects);
        assertFalse(capabilities.GL_KHR_debug);
        assertFalse(capabilities.GL_ARB_compute_shader);
    }

    @Test
    void aSecondInstallationIsRefusedOnceLwjglCountsALoadedLibrary() {
        DispatchInstaller late = new DispatchInstaller(binding, binding);

        assertThrows(IllegalStateException.class, () -> session.install(late, BindingGeneration.LWJGL2));
    }

    @Test
    void immediateModeFollowsTheMatrixStack() {
        projectOntoPixels();

        GL11.glPushMatrix();
        GL11.glTranslatef(WIDTH / 2.0F, 0.0F, 0.0F);
        GL11.glColor3f(0.0F, 1.0F, 0.0F);
        quad(0, 0, WIDTH / 2, HEIGHT);
        GL11.glPopMatrix();
        GL11.glColor3f(1.0F, 0.0F, 0.0F);
        quad(0, 0, WIDTH / 4, HEIGHT);

        assertEquals(GREEN, pixelAt(3 * WIDTH / 4, HEIGHT / 2));
        assertEquals(RED, pixelAt(WIDTH / 8, HEIGHT / 2));
        assertEquals(BLACK, pixelAt(3 * WIDTH / 8, HEIGHT / 2));
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test
    void aDisplayListReplaysItsCommandsAndLivesUntilItIsDeleted() {
        projectOntoPixels();
        int list = GL11.glGenLists(1);

        GL11.glNewList(list, GL11.GL_COMPILE);
        GL11.glColor3f(0.0F, 1.0F, 0.0F);
        quad(0, 0, WIDTH, HEIGHT);
        GL11.glEndList();

        assertEquals(BLACK, pixelAt(WIDTH / 2, HEIGHT / 2));
        GL11.glCallList(list);
        assertEquals(GREEN, pixelAt(WIDTH / 2, HEIGHT / 2));
        assertTrue(GL11.glIsList(list));
        GL11.glDeleteLists(list, 1);
        assertFalse(GL11.glIsList(list));
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test
    void fixedFunctionTexturingModulatesTheVertexColor() {
        projectOntoPixels();
        ByteBuffer yellowTexel = BufferUtils.createByteBuffer(CHANNELS);
        yellowTexel.put(new byte[] {(byte) 0xFF, (byte) 0xFF, 0, (byte) 0xFF}).flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, GL11.glGenTextures());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, yellowTexel);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

        GL11.glColor3f(0.0F, 1.0F, 1.0F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(0.0F, 0.0F);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(WIDTH, 0.0F);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(WIDTH, HEIGHT);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(0.0F, HEIGHT);
        GL11.glEnd();

        assertEquals(GREEN, pixelAt(WIDTH / 2, HEIGHT / 2));
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test
    void theBindingDrawsTheSamePixelsAsTheSameCallsWithoutIt() {
        projectOntoPixels();
        GL11.glColor3f(0.0F, 1.0F, 0.0F);
        quad(WIDTH / 4, HEIGHT / 4, WIDTH / 2, HEIGHT / 2);
        byte[] throughTheBinding = sceneBytes();

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GlScene reference = new GlScene(layer);
        reference.call("glColor3f", GlWords.of(0.0F), GlWords.of(1.0F), GlWords.of(0.0F));
        reference.call("glBegin", GL11.GL_QUADS);
        reference.call("glVertex2f", GlWords.of(WIDTH / 4.0F), GlWords.of(HEIGHT / 4.0F));
        reference.call("glVertex2f", GlWords.of(3 * WIDTH / 4.0F), GlWords.of(HEIGHT / 4.0F));
        reference.call("glVertex2f", GlWords.of(3 * WIDTH / 4.0F), GlWords.of(3 * HEIGHT / 4.0F));
        reference.call("glVertex2f", GlWords.of(WIDTH / 4.0F), GlWords.of(3 * HEIGHT / 4.0F));
        reference.call("glEnd");

        assertArrayEquals(reference.pixels(WIDTH, HEIGHT), throughTheBinding);
        assertEquals(GREEN, pixelAt(WIDTH / 2, HEIGHT / 2));
        assertEquals(BLACK, pixelAt(WIDTH / 8, HEIGHT / 8));
    }

    private static void projectOntoPixels() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, WIDTH, 0.0, HEIGHT, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    private static void quad(int x, int y, int width, int height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    private static int pixelAt(int x, int y) {
        ByteBuffer pixel = BufferUtils.createByteBuffer(CHANNELS);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        return pixel.getInt(0);
    }

    private static byte[] sceneBytes() {
        ByteBuffer pixels = BufferUtils.createByteBuffer(WIDTH * HEIGHT * CHANNELS);
        GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        byte[] copied = new byte[pixels.remaining()];
        pixels.get(copied);
        return copied;
    }
}
