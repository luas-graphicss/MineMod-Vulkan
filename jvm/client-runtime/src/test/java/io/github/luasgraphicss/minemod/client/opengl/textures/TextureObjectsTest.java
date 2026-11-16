package io.github.luasgraphicss.minemod.client.opengl.textures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.client.support.RecordedStream;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import io.github.luasgraphicss.minemod.client.textures.TexturePublisher;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.contract.handle.HandleAllocator;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class TextureObjectsTest {
    private static final int MAX_TEXTURES = 8;
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int PROXY_TEXTURE_2D = 0x8064;
    private static final int SECOND_UNIT = 0x84C1;
    private static final int RGBA = 0x1908;
    private static final int RGBA8 = 0x8058;
    private static final int RED = 0x1903;
    private static final int DEPTH_COMPONENT = 0x1902;
    private static final int UNSIGNED_BYTE = 0x1401;
    private static final int FLOAT = 0x1406;
    private static final int MAGNIFICATION_FILTER = 0x2800;
    private static final int MINIFICATION_FILTER = 0x2801;
    private static final int WRAP_S = 0x2802;
    private static final int MAX_LEVEL = 0x813D;
    private static final int NEAREST = 0x2600;
    private static final int LINEAR = 0x2601;
    private static final int CLAMP_TO_EDGE = 0x812F;
    private static final int UNPACK_ALIGNMENT = 0x0CF5;
    private static final int LEVEL_WIDTH = 0x1000;
    private static final int LEVEL_HEIGHT = 0x1001;
    private static final int DEFINITION_LEVELS_OFFSET = 16;
    private static final int DEFINITION_SAMPLING_OFFSET = 20;
    private static final int WRITE_LEVEL_OFFSET = 8;
    private static final int WRITE_TEXELS_OFFSET = 40;
    private static final int LINEAR_SAMPLING = 1;
    private static final int LINEAR_BLENDED_REPEATED_SAMPLING = 7;
    private static final byte GRASS = (byte) 0x3A;

    private final TexturePublisher published = new TexturePublisher(
            new HandleAllocator(ResourceKind.TEXTURE, TexturePublisher.handlesFor(MAX_TEXTURES)), MAX_TEXTURES);
    private final TextureObjects textures = new TextureObjects(published);

    @Test
    void publishesATextureWhenItsFirstTexelsArrive() {
        int atlas = bindNewTexture();
        textures.setParameter(TEXTURE_2D, MAX_LEVEL, 1);
        textures.specify(TEXTURE_2D, 0, RGBA, 4, 4);
        textures.specify(TEXTURE_2D, 1, RGBA, 2, 2);
        assertFalse(published.find(atlas).isPresent());

        write(new TexelArea(1, 0, 0, 2, 2), RGBA, 4);
        RecordedStream stream = publish();

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        assertEquals(2, stream.payload(0).getInt(DEFINITION_LEVELS_OFFSET));
        assertEquals(1, stream.payload(1).getInt(WRITE_LEVEL_OFFSET));
        assertEquals(GRASS, stream.payload(1).get(WRITE_TEXELS_OFFSET));
    }

    @Test
    void samplesOnlyTheLevelsTheGameLimitsItTo() {
        bindNewTexture();
        textures.setParameter(TEXTURE_2D, MAX_LEVEL, 0);
        textures.specify(TEXTURE_2D, 0, RGBA, 4, 4);
        textures.specify(TEXTURE_2D, 1, RGBA, 2, 2);

        write(new TexelArea(0, 0, 0, 4, 4), RGBA, 4);

        assertEquals(1, publishedDefinition().getInt(DEFINITION_LEVELS_OFFSET));
    }

    @Test
    void samplesWithTheFiltersOfANewOpenGlTexture() {
        bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RGBA, 4, 4);

        write(new TexelArea(0, 0, 0, 4, 4), RGBA, 4);

        assertEquals(LINEAR_BLENDED_REPEATED_SAMPLING, publishedDefinition().getInt(DEFINITION_SAMPLING_OFFSET));
    }

    @Test
    void samplesWithTheParametersSetAroundTheFirstTexels() {
        bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RGBA, 4, 4);
        textures.setParameter(TEXTURE_2D, MINIFICATION_FILTER, LINEAR);
        textures.setParameter(TEXTURE_2D, MAGNIFICATION_FILTER, LINEAR);

        write(new TexelArea(0, 0, 0, 4, 4), RGBA, 4);
        textures.setParameter(TEXTURE_2D, WRAP_S, CLAMP_TO_EDGE);

        assertEquals(LINEAR_SAMPLING, publishedDefinition().getInt(DEFINITION_SAMPLING_OFFSET));
    }

    @Test
    void spreadsSingleChannelGlyphsOverEveryChannel() {
        bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RED, 2, 1);
        textures.setParameter(TEXTURE_2D, MAGNIFICATION_FILTER, NEAREST);
        textures.store(UNPACK_ALIGNMENT, 1);

        write(new TexelArea(0, 0, 0, 2, 1), RED, 1);
        RecordedStream stream = publish();
        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        ByteBuffer written = stream.payload(1);

        assertEquals(GRASS, written.get(WRITE_TEXELS_OFFSET));
        assertEquals(GRASS, written.get(WRITE_TEXELS_OFFSET + 3));
    }

    @Test
    void keepsAFramebufferAttachmentAwayFromTheScene() {
        int color = bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RGBA8, 854, 480);
        int depth = bindNewTexture();
        textures.specify(TEXTURE_2D, 0, DEPTH_COMPONENT, 854, 480);
        textures.write(TEXTURE_2D, new TexelArea(0, 0, 0, 1, 1), DEPTH_COMPONENT, FLOAT, ByteBuffer.allocate(4));

        assertFalse(published.find(color).isPresent());
        assertFalse(published.find(depth).isPresent());
        assertEquals(Collections.emptyList(), publish().opcodes());
    }

    @Test
    void replacesThePublishedTextureWhenItsStorageChanges() {
        int atlas = bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RGBA, 4, 4);
        write(new TexelArea(0, 0, 0, 4, 4), RGBA, 4);
        publish();
        ResourceHandle replaced = published.find(atlas).get();

        textures.specify(TEXTURE_2D, 0, RGBA, 8, 8);
        assertFalse(published.find(atlas).isPresent());
        write(new TexelArea(0, 0, 0, 8, 8), RGBA, 4);

        assertEquals(
                Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE, SceneOpcode.DESTROY_TEXTURE),
                publish().opcodes());
        assertNotEquals(replaced, published.find(atlas).get());
    }

    @Test
    void writesIntoTheTextureOfTheActiveUnit() {
        int atlas = bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RGBA, 4, 4);
        textures.activate(SECOND_UNIT);
        int lightmap = bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RGBA, 4, 4);

        write(new TexelArea(0, 0, 0, 4, 4), RGBA, 4);

        assertTrue(published.find(lightmap).isPresent());
        assertFalse(published.find(atlas).isPresent());
    }

    @Test
    void destroysThePublishedTextureWithItsObject() {
        int atlas = bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RGBA, 4, 4);
        write(new TexelArea(0, 0, 0, 4, 4), RGBA, 4);
        publish();

        textures.delete(atlas);

        assertEquals(Collections.singletonList(SceneOpcode.DESTROY_TEXTURE), publish().opcodes());
        assertEquals(0, textures.bound());
    }

    @Test
    void answersTheProbeForTheLargestTexture() {
        textures.specify(PROXY_TEXTURE_2D, 0, RGBA, 32768, 32768);
        assertEquals(0, textures.levelParameter(PROXY_TEXTURE_2D, 0, LEVEL_WIDTH));

        textures.specify(PROXY_TEXTURE_2D, 0, RGBA, 16384, 16384);
        assertEquals(16384, textures.levelParameter(PROXY_TEXTURE_2D, 0, LEVEL_WIDTH));
    }

    @Test
    void tellsTheSizeOfALevel() {
        bindNewTexture();
        textures.specify(TEXTURE_2D, 0, RGBA, 8, 4);
        textures.specify(TEXTURE_2D, 1, RGBA, 4, 2);

        assertEquals(4, textures.levelParameter(TEXTURE_2D, 1, LEVEL_WIDTH));
        assertEquals(2, textures.levelParameter(TEXTURE_2D, 1, LEVEL_HEIGHT));
        assertEquals(0, textures.levelParameter(TEXTURE_2D, 2, LEVEL_WIDTH));
    }

    @Test
    void tellsTheBytesAnAreaReadsFromTheGame() {
        assertEquals(32, textures.sourceBytes(RGBA, 4, 2));
        assertEquals(0, textures.sourceBytes(DEPTH_COMPONENT, 4, 2));
    }

    private int bindNewTexture() {
        int texture = textures.generate();
        textures.bind(texture);
        return texture;
    }

    private void write(TexelArea area, int format, int texelBytes) {
        byte[] texels = new byte[(int) (area.bytes() / TexelArea.TEXEL_BYTES * texelBytes)];
        Arrays.fill(texels, GRASS);
        textures.write(TEXTURE_2D, area, format, UNSIGNED_BYTE, ByteBuffer.wrap(texels));
    }

    private ByteBuffer publishedDefinition() {
        RecordedStream stream = publish();
        assertEquals(SceneOpcode.DEFINE_TEXTURE, stream.opcodes().get(0));
        return stream.payload(0);
    }

    private RecordedStream publish() {
        StreamBudget definitions = TexturePublisher.definitionBudgetFor(MAX_TEXTURES);
        RecordedStream stream = new RecordedStream();
        published.publishDefinitions(stream.scene(), definitions);
        published.publishTexels(stream.scene(), new StreamBudget(32 * 1024, 16));
        published.publishDestructions(stream.scene(), definitions);
        return stream;
    }
}
