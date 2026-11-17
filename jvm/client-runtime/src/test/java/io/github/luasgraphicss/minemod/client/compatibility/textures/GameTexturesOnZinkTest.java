package io.github.luasgraphicss.minemod.client.compatibility.textures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import io.github.luasgraphicss.minemod.client.textures.TexturePublisher;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.natives.memory.JniMemory;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.CompatibilitySession;
import io.github.luasgraphicss.minemod.compatibility.runtime.SessionContext;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.NativeBlock;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.OwnedSceneTextures;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.contract.handle.HandleAllocator;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
class GameTexturesOnZinkTest {
    private static final int GL_NO_ERROR = 0;
    private static final int GL_UNPACK_ROW_LENGTH = 0x0CF2;
    private static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE_WIDTH = 0x1000;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_NEAREST = 0x2600;
    private static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int MAX_TEXTURES = 8;
    private static final int SMALL_SIDE = 4;
    private static final int LARGE_SIDE = 8;
    private static final int CHANNELS = 4;

    @TempDir
    Path providerDirectory;

    private final SceneWithItsProvider scene = new SceneWithItsProvider();
    private final TextureObjects gameTextures = new TextureObjects(new TexturePublisher(
            new HandleAllocator(ResourceKind.TEXTURE, TexturePublisher.handlesFor(MAX_TEXTURES)), MAX_TEXTURES));
    private CompatibilitySession session;
    private SessionContext context;
    private MirroredTextures mirrored;
    private GlScene gl;

    @BeforeEach
    void mirrorTheTexturesOfTheGameIntoTheProviderOfAMod() throws Exception {
        session = ProviderTestEnvironment.open(ProviderTestEnvironment.stage(providerDirectory), ContextProfile.CORE);
        context = session.createContext(ProviderTestEnvironment.planFor(scene));
        CompatibilityLayer layer = context.layer();
        mirrored = new MirroredTextures(new LayerAliases(layer), new ProviderTextures(scene.provider, new JniMemory()));
        gameTextures.mirrorTo(mirrored);
        gl = new GlScene(layer);
        layer.beginFrame();
        layer.enterCallback();
    }

    @AfterEach
    void closeTheSession() {
        gameTextures.stopMirroring();
        mirrored.close();
        context.close();
        session.close();
    }

    @Test
    void aModReadsTheTexelsOfTheGameUnderTheNameTheGameUses() {
        int atlas = uploadedByTheGame(SMALL_SIDE, GlScene.GREEN);

        assertEquals(1, gl.call("glIsTexture", atlas));
        assertArrayEquals(GlScene.GREEN, texelThroughAFramebuffer(atlas));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void whatAModBoundAndTheWayItUnpacksSurviveAnUploadOfTheGame() {
        int ownTexture = gl.generate("glGenTextures");
        gl.call("glBindTexture", GL_TEXTURE_2D, ownTexture);
        gl.call("glPixelStorei", GL_UNPACK_ALIGNMENT, 8);
        gl.call("glPixelStorei", GL_UNPACK_ROW_LENGTH, 3);

        uploadedByTheGame(SMALL_SIDE, GlScene.GREEN);

        assertEquals(8, gl.integer(GL_UNPACK_ALIGNMENT));
        assertEquals(3, gl.integer(GL_UNPACK_ROW_LENGTH));
        gl.call("glPixelStorei", GL_UNPACK_ROW_LENGTH, 0);
        try (NativeBlock texels = new NativeBlock(SMALL_SIDE * SMALL_SIDE * CHANNELS)) {
            texels.setBytes(texelsOf(SMALL_SIDE, GlScene.MAGENTA));
            gl.call(
                    "glTexImage2D",
                    GL_TEXTURE_2D, 0, GL_RGBA8, SMALL_SIDE, SMALL_SIDE, 0, GL_RGBA, GL_UNSIGNED_BYTE, texels.address());
        }
        assertArrayEquals(GlScene.MAGENTA, texelThroughAFramebuffer(ownTexture));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aTextureTheGameRedefinesKeepsItsNameAndShowsItsNewTexels() {
        int atlas = uploadedByTheGame(SMALL_SIDE, GlScene.GREEN);
        assertArrayEquals(GlScene.GREEN, texelThroughAFramebuffer(atlas));

        gameTextures.bind(atlas);
        uploadToTheBoundTexture(LARGE_SIDE, GlScene.MAGENTA);

        assertArrayEquals(GlScene.MAGENTA, texelThroughAFramebuffer(atlas));
        gl.call("glBindTexture", GL_TEXTURE_2D, atlas);
        assertEquals(LARGE_SIDE, levelParameterOfTheBoundTexture(GL_TEXTURE_WIDTH));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void theFilterTheGameSetsReachesTheTextureAModBinds() {
        int atlas = gameTextures.generate();
        gameTextures.bind(atlas);
        gameTextures.setParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        uploadToTheBoundTexture(SMALL_SIDE, GlScene.GREEN);

        gl.call("glBindTexture", GL_TEXTURE_2D, atlas);

        try (NativeBlock filter = new NativeBlock(Integer.BYTES)) {
            gl.call("glGetTexParameteriv", GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter.address());
            assertEquals(GL_NEAREST, filter.intAt(0));
        }
    }

    @Test
    void aTextureAModRedefinesNoLongerFollowsTheGame() {
        int atlas = uploadedByTheGame(SMALL_SIDE, GlScene.GREEN);
        gl.call("glBindTexture", GL_TEXTURE_2D, atlas);
        try (NativeBlock texels = new NativeBlock(SMALL_SIDE * SMALL_SIDE * CHANNELS)) {
            texels.setBytes(texelsOf(SMALL_SIDE, GlScene.BLACK));
            gl.call(
                    "glTexImage2D",
                    GL_TEXTURE_2D, 0, GL_RGBA8, SMALL_SIDE, SMALL_SIDE, 0, GL_RGBA, GL_UNSIGNED_BYTE, texels.address());
        }

        gameTextures.bind(atlas);
        gameTextures.write(
                GL_TEXTURE_2D,
                new TexelArea(0, 0, 0, SMALL_SIDE, SMALL_SIDE),
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                ByteBuffer.wrap(texelsOf(SMALL_SIDE, GlScene.MAGENTA)));

        assertArrayEquals(GlScene.BLACK, texelThroughAFramebuffer(atlas));
        assertEquals(0, mirrored.mirroredCount());
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aTextureTheGameDeletesIsNoTextureOfAModAnymore() {
        int atlas = uploadedByTheGame(SMALL_SIDE, GlScene.GREEN);

        gameTextures.delete(atlas);

        assertEquals(0, gl.call("glIsTexture", atlas));
        assertEquals(0, mirrored.mirroredCount());
    }

    private int uploadedByTheGame(int side, int[] color) {
        int texture = gameTextures.generate();
        gameTextures.bind(texture);
        uploadToTheBoundTexture(side, color);
        return texture;
    }

    private void uploadToTheBoundTexture(int side, int[] color) {
        gameTextures.specify(GL_TEXTURE_2D, 0, GL_RGBA, side, side);
        gameTextures.write(
                GL_TEXTURE_2D,
                new TexelArea(0, 0, 0, side, side),
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                ByteBuffer.wrap(texelsOf(side, color)));
    }

    private int[] texelThroughAFramebuffer(int texture) {
        int framebuffer = gl.generate("glGenFramebuffers");
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, framebuffer);
        gl.call("glFramebufferTexture2D", GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        int[] texel = gl.pixelAt(1, 1);
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, 0);
        return texel;
    }

    private int levelParameterOfTheBoundTexture(int parameter) {
        try (NativeBlock answer = new NativeBlock(Integer.BYTES)) {
            gl.call("glGetTexLevelParameteriv", GL_TEXTURE_2D, 0, parameter, answer.address());
            return answer.intAt(0);
        }
    }

    private static byte[] texelsOf(int side, int[] color) {
        byte[] texels = new byte[side * side * CHANNELS];
        for (int index = 0; index < texels.length; index++) {
            texels[index] = (byte) color[index % CHANNELS];
        }
        return texels;
    }

    private static final class SceneWithItsProvider implements SceneSource {
        private final SceneSource scene =
                new OwnedSceneTextures(ProviderTestEnvironment.SCENE_WIDTH, ProviderTestEnvironment.SCENE_HEIGHT);
        private ProviderCalls provider;

        @Override
        public SceneTextures attach(ProviderCalls calls, ScratchInts scratch) {
            provider = calls;
            return scene.attach(calls, scratch);
        }
    }
}
