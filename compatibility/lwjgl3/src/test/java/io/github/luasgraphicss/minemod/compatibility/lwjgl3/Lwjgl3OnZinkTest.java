package io.github.luasgraphicss.minemod.compatibility.lwjgl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.DispatchInstaller;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.CompatibilitySession;
import io.github.luasgraphicss.minemod.compatibility.runtime.SessionContext;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
class Lwjgl3OnZinkTest {
    private static final int WIDTH = ProviderTestEnvironment.SCENE_WIDTH;
    private static final int HEIGHT = ProviderTestEnvironment.SCENE_HEIGHT;
    private static final int MAGENTA = 0xFFFF00FF;
    private static final int GREEN = 0xFF00FF00;
    private static final String VERTEX_SOURCE =
            "#version 150\nin vec2 position;\nvoid main() { gl_Position = vec4(position, 0.0, 1.0); }\n";
    private static final String FRAGMENT_SOURCE =
            "#version 150\nuniform vec4 tint;\nout vec4 color;\nvoid main() { color = tint; }\n";

    @TempDir
    Path providerDirectory;

    private final Lwjgl3Dispatch binding = new Lwjgl3Dispatch();
    private CompatibilitySession session;
    private SessionContext context;
    private CompatibilityLayer layer;
    private GLCapabilities capabilities;

    @BeforeEach
    void installTheDispatchBeforeLwjglResolvesAnything() throws Exception {
        session = ProviderTestEnvironment.open(ProviderTestEnvironment.stage(providerDirectory), ContextProfile.CORE);
        session.install(new DispatchInstaller(binding, binding), BindingGeneration.LWJGL3);
        context = session.createContext(ProviderTestEnvironment.standalonePlan());
        layer = context.layer();
        capabilities = GL.createCapabilities();
        layer.beginFrame();
        layer.enterCallback();
        GL11.glViewport(0, 0, WIDTH, HEIGHT);
    }

    @AfterEach
    void removeTheDispatch() {
        binding.uninstall();
        context.close();
        session.close();
    }

    @Test
    void lwjglSeesOnlyTheExtensionsTheLayerExposes() {
        assertTrue(capabilities.OpenGL11);
        assertEquals(session.exposedExtensions().contains("GL_ARB_sampler_objects"), capabilities.GL_ARB_sampler_objects);
        assertFalse(capabilities.GL_KHR_debug);
        assertFalse(capabilities.GL_ARB_compute_shader);
        assertEquals(session.exposedExtensions().size(), GL11.glGetInteger(GL30.GL_NUM_EXTENSIONS));
    }

    @Test
    void aSecondInstallationIsRefusedOnceLwjglHoldsAFunctionProvider() {
        DispatchInstaller late = new DispatchInstaller(binding, binding);

        assertThrows(IllegalStateException.class, () -> session.install(late, BindingGeneration.LWJGL3));
    }

    @Test
    void namesThatLwjglReceivesAreVirtualAndBindToTheirOwnObjects() {
        int first = GL11.glGenTextures();
        int second = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, second);

        assertEquals(1, first);
        assertEquals(2, second);
        assertEquals(second, GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
        assertTrue(GL11.glIsTexture(second));
        assertFalse(GL11.glIsTexture(first));

        GL11.glDeleteTextures(second);
        assertFalse(GL11.glIsTexture(second));
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test
    void theDefaultFramebufferOfLwjglIsTheSceneOfTheLayer() {
        GL11.glClearColor(1.0F, 0.0F, 1.0F, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        assertEquals(MAGENTA, pixelAt(WIDTH / 2, HEIGHT / 2));
        assertEquals(0, GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING));

        int framebuffer = GL30.glGenFramebuffers();
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 4, 4, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        GL11.glClearColor(0.0F, 1.0F, 0.0F, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        assertEquals(GL30.GL_FRAMEBUFFER_COMPLETE, GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
        assertEquals(GREEN, pixelAt(1, 1));
        assertEquals(framebuffer, GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING));
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        assertEquals(MAGENTA, pixelAt(1, 1));
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test
    void aProgramCompiledThroughLwjglDrawsIntoTheScene() {
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, compiled(GL20.GL_VERTEX_SHADER, VERTEX_SOURCE));
        GL20.glAttachShader(program, compiled(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SOURCE));
        GL20.glBindAttribLocation(program, 0, "position");
        GL20.glLinkProgram(program);
        assertEquals(GL11.GL_TRUE, GL20.glGetProgrami(program, GL20.GL_LINK_STATUS), GL20.glGetProgramInfoLog(program));

        FloatBuffer coveringTriangle = BufferUtils.createFloatBuffer(6).put(new float[] {-1, -1, 3, -1, -1, 3});
        coveringTriangle.flip();
        GL30.glBindVertexArray(GL30.glGenVertexArrays());
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL15.glGenBuffers());
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, coveringTriangle, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0L);
        GL20.glEnableVertexAttribArray(0);
        GL20.glUseProgram(program);
        GL20.glUniform4f(GL20.glGetUniformLocation(program, "tint"), 0.0F, 1.0F, 0.0F, 1.0F);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        assertEquals(GREEN, pixelAt(WIDTH / 2, HEIGHT / 2));
        assertEquals(program, GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test
    void aShaderThatDoesNotCompileKeepsItsLogAndTheErrorOrder() {
        int shader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(shader, "#version 150\nvoid main() { this is not GLSL }\n");
        GL20.glCompileShader(shader);

        assertEquals(GL11.GL_FALSE, GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS));
        assertFalse(GL20.glGetShaderInfoLog(shader).isEmpty());

        GL11.glEnable(0x7FFF);
        assertEquals(GL11.GL_INVALID_ENUM, GL11.glGetError());
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    private static int compiled(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        assertEquals(GL11.GL_TRUE, GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS), GL20.glGetShaderInfoLog(shader));
        return shader;
    }

    private static int pixelAt(int x, int y) {
        ByteBuffer pixel = BufferUtils.createByteBuffer(Integer.BYTES);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        return pixel.getInt(0);
    }
}
