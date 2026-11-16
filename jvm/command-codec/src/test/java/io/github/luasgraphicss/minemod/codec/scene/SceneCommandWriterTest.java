package io.github.luasgraphicss.minemod.codec.scene;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.codec.CommandStreamCorpus;
import io.github.luasgraphicss.minemod.codec.CommandStreamValidator;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SceneCommandWriterTest {
    private static final int STREAM_CAPACITY = 4096;
    private static final int FRAME_COMMANDS = 15;
    private static final ResourceHandle ATLAS = ResourceHandle.of(ResourceKind.TEXTURE, 1, 1);
    private static final ResourceHandle LIGHTMAP = ResourceHandle.of(ResourceKind.TEXTURE, 1, 2);
    private static final ResourceHandle SPRITE = ResourceHandle.of(ResourceKind.TEXTURE, 1, 3);
    private static final ResourceHandle BUFFER = ResourceHandle.of(ResourceKind.BUFFER, 1, 0);
    private static final Set<TextureSampling> NEAREST = EnumSet.noneOf(TextureSampling.class);
    private static final Set<VertexElement> SPRITE_FORMAT =
            EnumSet.of(VertexElement.POSITION, VertexElement.COLOR, VertexElement.TEXTURE);
    private static final TextureRegion ONE_TEXEL = new TextureRegion(0, 0, 0, 1, 1);
    private static final TextureRegion TWO_TEXELS = new TextureRegion(0, 0, 0, 2, 1);
    private static final Fog DISTANT_FOG = new Fog(0.0f, 0.0f, 0.0f, 1.0f, 1000.0f, 2000.0f, FogShape.SPHERE);
    private static final float[] PROJECTION = {
        4.0f, 0.0f, 0.0f, 0.0f, 0.0f, 4.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0078125f, -1.0f, 0.0f, 0.0f, -1.0078125f, 0.0f,
    };
    private static final float[] CANVAS = {
        0.03125f, 0.0f, 0.0f, 0.0f, 0.0f, -0.03125f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f,
    };
    private static final float[][] WALL_CORNERS = {{4.0f, 0.0f}, {16.0f, 0.0f}, {16.0f, 16.0f}, {4.0f, 16.0f}};
    private static final float[][] SPRITE_CORNERS = {
        {0.0f, 0.0f, 0.0f, 0.0f}, {0.0f, 16.0f, 0.0f, 1.0f}, {16.0f, 16.0f, 1.0f, 1.0f}, {16.0f, 0.0f, 1.0f, 0.0f},
    };
    private static final int[] WALL_INDICES = {0, 1, 2, 0, 2, 3};
    private static final int[] SOLID_LAYER_ONLY = {0, 6, 0, 0, 0, 0, 0, 0};
    private static final float WALL_DEPTH = 8.0f;
    private static final float SPRITE_DEPTH = 0.5f;
    private static final short FULL_LIGHT = 240;
    private static final byte OPAQUE = (byte) 0xFF;

    @Test
    void writesTheFrameOfTheCorpusByteForByte() throws IOException {
        ByteBuffer written = corpusFrame();
        byte[] bytes = new byte[written.remaining()];
        written.get(bytes);

        assertArrayEquals(CommandStreamCorpus.named("valid-scene-frame.stream").bytes, bytes);
    }

    @Test
    void writesStreamsTheValidatorAccepts() {
        assertEquals(StreamReport.accepted(FRAME_COMMANDS), CommandStreamValidator.validate(corpusFrame()));
    }

    @Test
    void refusesVerticesThatAreNotWholeVertices() {
        SceneCommandWriter writer = newWriter();
        ByteBuffer vertices = ByteBuffer.allocate(spriteVertices().remaining() - 1);

        assertThrows(IllegalArgumentException.class, () -> writer.drawBatch(spriteBatch(), vertices));
    }

    @Test
    void refusesTexelsThatDoNotFillTheirRegion() {
        SceneCommandWriter writer = newWriter();

        assertThrows(IllegalArgumentException.class, () -> writer.writeTexture(SPRITE, TWO_TEXELS, texels(OPAQUE)));
    }

    @Test
    void refusesAHandleThatIsNotATexture() {
        SceneCommandWriter writer = newWriter();

        assertThrows(IllegalArgumentException.class, () -> writer.defineTexture(BUFFER, 1, 1, 1, NEAREST));
        assertThrows(IllegalArgumentException.class, () -> writer.destroyTexture(BUFFER));
    }

    @Test
    void refusesAFogThatEndsWhereItStarts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Fog(0.0f, 0.0f, 0.0f, 1.0f, 16.0f, 16.0f, FogShape.CYLINDER));
    }

    @Test
    void refusesAFormatWithoutPositionAndAViewOutsideTheFrame() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BatchState.builder(BatchSlot.SOLID, 0, EnumSet.of(VertexElement.COLOR), PrimitiveMode.QUADS));
        assertThrows(
                IllegalArgumentException.class,
                () -> BatchState.builder(BatchSlot.SOLID, ViewState.MAX_VIEWS, SPRITE_FORMAT, PrimitiveMode.QUADS));
        assertThrows(IllegalArgumentException.class, () -> ViewState.builder(-1, CANVAS, DISTANT_FOG));
    }

    @Test
    void refusesASectionWhoseVerticesAreNotWhole() {
        ByteBuffer vertices = ByteBuffer.allocate(SectionMesh.VERTEX_BYTES + 1);

        assertThrows(
                IllegalArgumentException.class, () -> new SectionMesh(vertices, wallIndices(), SOLID_LAYER_ONLY));
    }

    private static ByteBuffer corpusFrame() {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY));
        new SceneCommandWriter(stream)
                .defineTexture(ATLAS, 1, 1, 1, NEAREST)
                .writeTexture(ATLAS, ONE_TEXEL, texels(OPAQUE, OPAQUE, OPAQUE, OPAQUE))
                .defineTexture(LIGHTMAP, 1, 1, 1, EnumSet.of(TextureSampling.LINEAR))
                .writeTexture(LIGHTMAP, ONE_TEXEL, texels(OPAQUE, OPAQUE, OPAQUE, OPAQUE))
                .defineTexture(SPRITE, 2, 1, 1, NEAREST)
                .writeTexture(SPRITE, TWO_TEXELS, texels(OPAQUE, 0, 0, OPAQUE, 0, 0, OPAQUE, OPAQUE))
                .publishSection(0, 0, -3, 0, new SectionMesh(wallVertices(), wallIndices(), SOLID_LAYER_ONLY))
                .beginWorldFrame(0.125f, 0.125f, 0.125f, 1.0f)
                .setTerrain(new TerrainState(PROJECTION, 8.0, 8.0, 0.0, DISTANT_FOG, ATLAS, LIGHTMAP))
                .setView(ViewState.builder(0, CANVAS, DISTANT_FOG).build())
                .drawBatch(spriteBatch(), spriteVertices())
                .endFrame()
                .unloadSection(0, 0, -3)
                .changeWorld()
                .destroyTexture(SPRITE);
        return stream.finish();
    }

    private static SceneCommandWriter newWriter() {
        return new SceneCommandWriter(new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY)));
    }

    private static BatchState spriteBatch() {
        return BatchState.builder(BatchSlot.INTERFACE, 0, SPRITE_FORMAT, PrimitiveMode.QUADS)
                .culling(FaceCulling.DISABLED)
                .shading(EnumSet.of(Shading.TEXTURE))
                .texture(SPRITE)
                .build();
    }

    private static ByteBuffer wallVertices() {
        ByteBuffer vertices = littleEndian(WALL_CORNERS.length * SectionMesh.VERTEX_BYTES);
        for (float[] corner : WALL_CORNERS) {
            vertices.putFloat(corner[0]).putFloat(corner[1]).putFloat(WALL_DEPTH);
            vertices.put(OPAQUE).put((byte) 0).put((byte) 0).put(OPAQUE);
            vertices.putFloat(0.5f).putFloat(0.5f).putShort(FULL_LIGHT).putShort(FULL_LIGHT);
        }
        vertices.flip();
        return vertices;
    }

    private static ByteBuffer wallIndices() {
        ByteBuffer indices = littleEndian(WALL_INDICES.length * Integer.BYTES);
        for (int index : WALL_INDICES) {
            indices.putInt(index);
        }
        indices.flip();
        return indices;
    }

    private static ByteBuffer spriteVertices() {
        ByteBuffer vertices = littleEndian(SPRITE_CORNERS.length * VertexElement.strideOf(SPRITE_FORMAT));
        for (float[] corner : SPRITE_CORNERS) {
            vertices.putFloat(corner[0]).putFloat(corner[1]).putFloat(SPRITE_DEPTH);
            vertices.put(OPAQUE).put(OPAQUE).put(OPAQUE).put(OPAQUE);
            vertices.putFloat(corner[2]).putFloat(corner[3]);
        }
        vertices.flip();
        return vertices;
    }

    private static ByteBuffer texels(int... channels) {
        ByteBuffer texels = ByteBuffer.allocate(channels.length);
        for (int channel : channels) {
            texels.put((byte) channel);
        }
        texels.flip();
        return texels;
    }

    private static ByteBuffer littleEndian(int bytes) {
        return ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }
}
