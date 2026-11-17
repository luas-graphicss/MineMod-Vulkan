package io.github.luasgraphicss.minemod.compatibility.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportedOperation;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.NativeBlock;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.OwnedSceneTextures;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.RendererTextureStandIn;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
class SharedTexturesOnZinkTest {
    private static final int GL_NO_ERROR = 0;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE_WIDTH = 0x1000;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final long RENDERER_TEXTURE = 0x51;
    private static final int REDEFINED_SIDE = 8;

    @TempDir
    Path providerDirectory;

    private final RendererTextureStandIn rendererTexture = new RendererTextureStandIn(
            new OwnedSceneTextures(ProviderTestEnvironment.SCENE_WIDTH, ProviderTestEnvironment.SCENE_HEIGHT),
            GlScene.GREEN);
    private CompatibilitySession session;
    private SessionContext context;
    private CompatibilityLayer layer;
    private GlScene gl;

    @BeforeEach
    void openAContextNextToATextureOfTheRenderer() throws Exception {
        session = ProviderTestEnvironment.open(ProviderTestEnvironment.stage(providerDirectory), ContextProfile.CORE);
        context = session.createContext(ProviderTestEnvironment.planFor(rendererTexture));
        layer = context.layer();
        gl = new GlScene(layer);
        layer.beginFrame();
        layer.enterCallback();
    }

    @AfterEach
    void closeTheSession() {
        context.close();
        session.close();
    }

    @Test
    void aModReadsAPublishedTextureUnderTheNameItWasGiven() {
        int name = layer.publishVanillaTexture(RENDERER_TEXTURE, rendererTexture.backing());

        assertEquals(1, gl.call("glIsTexture", name));
        assertArrayEquals(GlScene.GREEN, texelThroughAFramebuffer(name));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void redefiningAPublishedTextureMovesTheModToStorageOfItsOwn() {
        int name = layer.publishVanillaTexture(RENDERER_TEXTURE, rendererTexture.backing());
        gl.call("glBindTexture", GL_TEXTURE_2D, name);

        gl.call(
                "glTexImage2D",
                GL_TEXTURE_2D, 0, GL_RGBA8, REDEFINED_SIDE, REDEFINED_SIDE, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);

        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
        assertEquals(REDEFINED_SIDE, widthOfTheBoundTexture());
        assertEquals(name, gl.integer(GL_TEXTURE_BINDING_2D));
        assertEquals(RendererTextureStandIn.SIDE, rendererTexture.widthInProvider());
        List<String> reported = reported();
        assertTrue(reported.stream().anyMatch(line -> line.contains("glTexImage2D on a vanilla texture")), reported.toString());
    }

    @Test
    void theImportOfARedefinedTextureLivesUntilItsFrameIsPresented() {
        int name = layer.publishVanillaTexture(RENDERER_TEXTURE, rendererTexture.backing());
        gl.call("glBindTexture", GL_TEXTURE_2D, name);
        gl.call(
                "glTexImage2D",
                GL_TEXTURE_2D, 0, GL_RGBA8, REDEFINED_SIDE, REDEFINED_SIDE, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);

        assertTrue(rendererTexture.isTextureInProvider(rendererTexture.backing()));
        layer.exitCallback();
        layer.present();
        assertFalse(rendererTexture.isTextureInProvider(rendererTexture.backing()));
    }

    @Test
    void aTextureDeletedInsideAFrameKeepsItsStorageUntilThatFrameIsPresented() {
        int texture = gl.generate("glGenTextures");
        gl.call("glBindTexture", GL_TEXTURE_2D, texture);
        gl.call("glTexImage2D", GL_TEXTURE_2D, 0, GL_RGBA8, 4, 4, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        int backing = context.context().names().table(ObjectKind.TEXTURE).backingOf(texture);

        try (NativeBlock deleted = new NativeBlock(Integer.BYTES)) {
            deleted.setInt(0, texture);
            gl.call("glDeleteTextures", 1, deleted.address());
        }

        assertEquals(0, gl.call("glIsTexture", texture));
        assertTrue(rendererTexture.isTextureInProvider(backing));
        layer.exitCallback();
        layer.present();
        assertFalse(rendererTexture.isTextureInProvider(backing));
    }

    private int[] texelThroughAFramebuffer(int texture) {
        int framebuffer = gl.generate("glGenFramebuffers");
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, framebuffer);
        gl.call("glFramebufferTexture2D", GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        int[] texel = gl.pixelAt(1, 1);
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, 0);
        return texel;
    }

    private int widthOfTheBoundTexture() {
        try (NativeBlock width = new NativeBlock(Integer.BYTES)) {
            gl.call("glGetTexLevelParameteriv", GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width.address());
            return width.intAt(0);
        }
    }

    private List<String> reported() {
        return layer.reports().reportedOperations().stream()
                .map(ReportedOperation::toString)
                .collect(Collectors.toList());
    }
}
