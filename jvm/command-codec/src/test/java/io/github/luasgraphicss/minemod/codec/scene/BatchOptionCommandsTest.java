package io.github.luasgraphicss.minemod.codec.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.codec.CommandStreamValidator;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class BatchOptionCommandsTest {
    private static final int STREAM_CAPACITY = 256;
    private static final int FIRST_RECORD = 16;
    private static final int RECORD_HEADER_BYTES = 8;
    private static final int RECORD_BYTES_FIELD = FIRST_RECORD + 4;
    private static final int FIRST_PAYLOAD = FIRST_RECORD + RECORD_HEADER_BYTES;
    private static final float[] RIGHT_HALF_OF_U = {0.5f, 0.0f, 0.5f};
    private static final float[] UNCHANGED_V = {0.0f, 1.0f, 0.0f};
    private static final float[] TWO_VALUES = {1.0f, 0.0f};

    @Test
    void writesAClipAsALowerLeftRectangle() {
        ByteBuffer stream = streamOf(writer -> writer.setClip(-4, 8, 32, 16));

        assertEquals(27, stream.getShort(FIRST_RECORD));
        assertEquals(RECORD_HEADER_BYTES + 16, stream.getInt(RECORD_BYTES_FIELD));
        assertEquals(-4, stream.getInt(FIRST_PAYLOAD));
        assertEquals(8, stream.getInt(FIRST_PAYLOAD + 4));
        assertEquals(32, stream.getInt(FIRST_PAYLOAD + 8));
        assertEquals(16, stream.getInt(FIRST_PAYLOAD + 12));
    }

    @Test
    void writesAClearedClipWithoutFields() {
        ByteBuffer stream = streamOf(SceneCommandWriter::clearClip);

        assertEquals(28, stream.getShort(FIRST_RECORD));
        assertEquals(RECORD_HEADER_BYTES, stream.getInt(RECORD_BYTES_FIELD));
    }

    @Test
    void writesTheURowOfATextureTransformBeforeItsVRow() {
        ByteBuffer stream = streamOf(writer -> writer.setTextureTransform(RIGHT_HALF_OF_U, UNCHANGED_V));

        assertEquals(29, stream.getShort(FIRST_RECORD));
        assertEquals(RECORD_HEADER_BYTES + 24, stream.getInt(RECORD_BYTES_FIELD));
        for (int value = 0; value < RIGHT_HALF_OF_U.length; value++) {
            assertEquals(RIGHT_HALF_OF_U[value], stream.getFloat(FIRST_PAYLOAD + value * Float.BYTES));
            assertEquals(UNCHANGED_V[value], stream.getFloat(FIRST_PAYLOAD + (3 + value) * Float.BYTES));
        }
    }

    @Test
    void writesAQuadOrderByItsCode() {
        ByteBuffer stream = streamOf(writer -> writer.setQuadOrder(QuadOrder.RISING_DEPTH));

        assertEquals(30, stream.getShort(FIRST_RECORD));
        assertEquals(RECORD_HEADER_BYTES + 8, stream.getInt(RECORD_BYTES_FIELD));
        assertEquals(2, stream.getInt(FIRST_PAYLOAD));
        assertEquals(0, stream.getInt(FIRST_PAYLOAD + 4));
    }

    @Test
    void writesOptionsTheValidatorAccepts() {
        ByteBuffer stream = streamOf(writer -> writer.setClip(0, 0, 8, 8)
                .setTextureTransform(RIGHT_HALF_OF_U, UNCHANGED_V)
                .setQuadOrder(QuadOrder.FARTHEST_FIRST)
                .clearClip());

        assertEquals(StreamReport.accepted(4), CommandStreamValidator.validate(stream));
    }

    @Test
    void refusesAClipWithANegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> streamOf(writer -> writer.setClip(0, 0, -1, 8)));
        assertThrows(IllegalArgumentException.class, () -> streamOf(writer -> writer.setClip(0, 0, 8, -1)));
    }

    @Test
    void refusesATextureRowThatDoesNotHoldThreeValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> streamOf(writer -> writer.setTextureTransform(TWO_VALUES, UNCHANGED_V)));
        assertThrows(
                IllegalArgumentException.class,
                () -> streamOf(writer -> writer.setTextureTransform(RIGHT_HALF_OF_U, TWO_VALUES)));
    }

    @Test
    void numbersTheInterfaceBlendingsAfterCrumbling() {
        assertEquals(6, Blending.INVERSION.code());
        assertEquals(7, Blending.DARKENING.code());
    }

    private static ByteBuffer streamOf(Consumer<SceneCommandWriter> commands) {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY));
        commands.accept(new SceneCommandWriter(stream));
        return stream.finish();
    }
}
