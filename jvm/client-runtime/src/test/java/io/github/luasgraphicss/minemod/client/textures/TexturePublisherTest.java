package io.github.luasgraphicss.minemod.client.textures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.client.support.RecordedStream;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.codec.scene.TextureSampling;
import io.github.luasgraphicss.minemod.contract.handle.HandleAllocator;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TexturePublisherTest {
    private static final int MAX_TEXTURES = 4;
    private static final int ATLAS = 7;
    private static final int FONT = 9;
    private static final TextureShape FOUR_BY_FOUR = new TextureShape(4, 4, 1);
    private static final TexelArea WHOLE = new TexelArea(0, 0, 0, 4, 4);
    private static final Set<TextureSampling> NEAREST = EnumSet.noneOf(TextureSampling.class);
    private static final int DEFINITION_SAMPLING_OFFSET = 20;
    private static final int LINEAR_SAMPLING = 1;
    private static final int WRITE_Y_OFFSET = 16;
    private static final int WRITE_HEIGHT_OFFSET = 24;
    private static final int WRITE_TEXELS_OFFSET = 40;
    private static final int ROW_BYTES = 16;
    private static final long WRITE_BYTES = SceneOpcode.WRITE_TEXTURE.fixedBytes();
    private static final byte GRASS = (byte) 0x3A;
    private static final byte STONE = (byte) 0x7F;

    private final HandleAllocator handles =
            new HandleAllocator(ResourceKind.TEXTURE, TexturePublisher.handlesFor(MAX_TEXTURES));
    private final TexturePublisher textures = new TexturePublisher(handles, MAX_TEXTURES);

    @Test
    void definesATextureBeforeItsTexels() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        textures.write(ATLAS, WHOLE, texelsOf(WHOLE, GRASS));

        RecordedStream stream = publish(roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        assertEquals(textures.find(ATLAS).get().bits(), stream.payload(0).getLong(0));
        assertEquals(GRASS, stream.payload(1).get(WRITE_TEXELS_OFFSET));
    }

    @Test
    void writesTheRowsThatFitAndTheRestInTheNextStream() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        textures.write(ATLAS, WHOLE, texelsOf(WHOLE, GRASS));

        RecordedStream first = publish(new StreamBudget(16 + WRITE_BYTES + 3 * ROW_BYTES, 1));
        RecordedStream second = publish(roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), first.opcodes());
        assertEquals(3, first.payload(1).getInt(WRITE_HEIGHT_OFFSET));
        assertEquals(Collections.singletonList(SceneOpcode.WRITE_TEXTURE), second.opcodes());
        assertEquals(3, second.payload(0).getInt(WRITE_Y_OFFSET));
        assertEquals(1, second.payload(0).getInt(WRITE_HEIGHT_OFFSET));
    }

    @Test
    void writesOnlyTheLatestTexelsOfAnAreaThatIsStillWaiting() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        textures.write(ATLAS, WHOLE, texelsOf(WHOLE, GRASS));
        textures.write(ATLAS, WHOLE, texelsOf(WHOLE, STONE));

        RecordedStream stream = publish(roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        assertEquals(STONE, stream.payload(1).get(WRITE_TEXELS_OFFSET));
    }

    @Test
    void writesLaterTexelsAfterTheOnesThatArePartlyWritten() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        textures.write(ATLAS, WHOLE, texelsOf(WHOLE, GRASS));
        publish(new StreamBudget(16 + WRITE_BYTES + 2 * ROW_BYTES, 1));
        textures.write(ATLAS, WHOLE, texelsOf(WHOLE, STONE));

        RecordedStream stream = publish(roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.WRITE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        assertEquals(GRASS, stream.payload(0).get(WRITE_TEXELS_OFFSET));
        assertEquals(2, stream.payload(0).getInt(WRITE_Y_OFFSET));
        assertEquals(STONE, stream.payload(1).get(WRITE_TEXELS_OFFSET));
        assertEquals(0, stream.payload(1).getInt(WRITE_Y_OFFSET));
    }

    @Test
    void destroysAPublishedTextureAndFreesItsHandle() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        publish(roomyBudget());
        textures.destroy(ATLAS);

        RecordedStream stream = publish(roomyBudget());

        assertEquals(Collections.singletonList(SceneOpcode.DESTROY_TEXTURE), stream.opcodes());
        assertFalse(textures.find(ATLAS).isPresent());
        assertEquals(0, handles.liveCount());
    }

    @Test
    void destroysATextureAfterTheTexelsOfItsReplacement() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        publish(roomyBudget());
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        textures.write(ATLAS, WHOLE, texelsOf(WHOLE, STONE));

        RecordedStream stream = publish(roomyBudget());

        assertEquals(
                Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE, SceneOpcode.DESTROY_TEXTURE),
                stream.opcodes());
    }

    @Test
    void forgetsATextureThatTheSceneNeverSaw() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        textures.write(ATLAS, WHOLE, texelsOf(WHOLE, GRASS));
        textures.destroy(ATLAS);

        RecordedStream stream = publish(roomyBudget());

        assertEquals(Collections.emptyList(), stream.opcodes());
        assertEquals(0, handles.liveCount());
    }

    @Test
    void redefinesATextureUnderANewHandle() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        publish(roomyBudget());
        ResourceHandle replaced = textures.find(ATLAS).get();
        textures.define(ATLAS, new TextureShape(8, 8, 2), NEAREST);

        RecordedStream stream = publish(roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.DESTROY_TEXTURE), stream.opcodes());
        assertEquals(replaced.bits(), stream.payload(1).getLong(0));
        assertNotEquals(replaced, textures.find(ATLAS).get());
    }

    @Test
    void definesWithTheSamplingChosenBeforeTheSceneSawTheTexture() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        textures.sample(ATLAS, EnumSet.of(TextureSampling.LINEAR));

        RecordedStream stream = publish(roomyBudget());

        assertEquals(Collections.singletonList(SceneOpcode.DEFINE_TEXTURE), stream.opcodes());
        assertEquals(LINEAR_SAMPLING, stream.payload(0).getInt(DEFINITION_SAMPLING_OFFSET));
    }

    @Test
    void refusesTexelsOutsideTheTexture() {
        textures.define(ATLAS, FOUR_BY_FOUR, NEAREST);
        TexelArea beyond = new TexelArea(0, 2, 2, 4, 4);
        textures.write(ATLAS, beyond, texelsOf(beyond, GRASS));
        textures.write(FONT, WHOLE, texelsOf(WHOLE, GRASS));

        RecordedStream stream = publish(roomyBudget());

        assertEquals(Collections.singletonList(SceneOpcode.DEFINE_TEXTURE), stream.opcodes());
        assertEquals(2, textures.refusedWrites());
    }

    @Test
    void refusesTexturesBeyondTheLimitOfTheScene() {
        for (int texture = 0; texture < MAX_TEXTURES; texture++) {
            textures.define(texture, FOUR_BY_FOUR, NEAREST);
        }
        textures.define(FONT, FOUR_BY_FOUR, NEAREST);

        assertFalse(textures.find(FONT).isPresent());
        assertEquals(1, textures.refusedTextures());
        assertEquals(MAX_TEXTURES, textures.liveTextures());
    }

    @Test
    void reservesADefinitionAndADestructionForEveryTexture() {
        for (int round = 0; round < 2; round++) {
            for (int texture = 0; texture < MAX_TEXTURES; texture++) {
                textures.define(texture, FOUR_BY_FOUR, NEAREST);
            }
            StreamBudget definitions = TexturePublisher.definitionBudgetFor(MAX_TEXTURES);
            RecordedStream stream = new RecordedStream();
            textures.publishDefinitions(stream.scene(), definitions);
            textures.publishDestructions(stream.scene(), definitions);
            assertEquals(round == 0 ? MAX_TEXTURES : 2 * MAX_TEXTURES, stream.opcodes().size());
        }
        assertTrue(textures.find(0).isPresent());
    }

    private RecordedStream publish(StreamBudget texels) {
        StreamBudget definitions = TexturePublisher.definitionBudgetFor(MAX_TEXTURES);
        RecordedStream stream = new RecordedStream();
        textures.publishDefinitions(stream.scene(), definitions);
        textures.publishTexels(stream.scene(), texels);
        textures.publishDestructions(stream.scene(), definitions);
        return stream;
    }

    private static StreamBudget roomyBudget() {
        return new StreamBudget(32 * 1024, 16);
    }

    private static ByteBuffer texelsOf(TexelArea area, byte value) {
        byte[] texels = new byte[(int) area.bytes()];
        Arrays.fill(texels, value);
        return ByteBuffer.wrap(texels);
    }
}
