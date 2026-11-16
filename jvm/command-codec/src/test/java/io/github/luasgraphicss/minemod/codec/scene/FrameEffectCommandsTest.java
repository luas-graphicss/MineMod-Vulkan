package io.github.luasgraphicss.minemod.codec.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.codec.CommandStreamValidator;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class FrameEffectCommandsTest {
    private static final int STREAM_CAPACITY = 256;
    private static final int FIRST_RECORD = 16;
    private static final int RECORD_HEADER_BYTES = 8;
    private static final int RECORD_BYTES_FIELD = FIRST_RECORD + 4;
    private static final int FIRST_PAYLOAD = FIRST_RECORD + RECORD_HEADER_BYTES;
    private static final float FIVE_PIXELS = 5.0f;
    private static final int THRICE = 3;

    @Test
    void writesADepthClearWithoutFields() {
        ByteBuffer stream = streamOf(SceneCommandWriter::clearDepth);

        assertEquals(32, stream.getShort(FIRST_RECORD));
        assertEquals(RECORD_HEADER_BYTES, stream.getInt(RECORD_BYTES_FIELD));
    }

    @Test
    void writesTheRadiusOfABlurBeforeItsRepeats() {
        ByteBuffer stream = streamOf(writer -> writer.blurFrame(new FrameBlur(FIVE_PIXELS, THRICE)));

        assertEquals(33, stream.getShort(FIRST_RECORD));
        assertEquals(RECORD_HEADER_BYTES + 8, stream.getInt(RECORD_BYTES_FIELD));
        assertEquals(FIVE_PIXELS, stream.getFloat(FIRST_PAYLOAD));
        assertEquals(THRICE, stream.getInt(FIRST_PAYLOAD + 4));
    }

    @Test
    void writesEffectsTheValidatorAccepts() {
        ByteBuffer stream =
                streamOf(writer -> writer.clearDepth().blurFrame(new FrameBlur(FrameBlur.MAX_RADIUS, FrameBlur.MAX_REPEATS)));

        assertEquals(StreamReport.accepted(2), CommandStreamValidator.validate(stream));
    }

    @Test
    void refusesABlurWithoutAMeasuredRadius() {
        assertThrows(IllegalArgumentException.class, () -> new FrameBlur(-1.0f, THRICE));
        assertThrows(IllegalArgumentException.class, () -> new FrameBlur(FrameBlur.MAX_RADIUS + 1.0f, THRICE));
        assertThrows(IllegalArgumentException.class, () -> new FrameBlur(Float.NaN, THRICE));
    }

    @Test
    void refusesABlurRepeatedNeverOrTooOften() {
        assertThrows(IllegalArgumentException.class, () -> new FrameBlur(FIVE_PIXELS, 0));
        assertThrows(IllegalArgumentException.class, () -> new FrameBlur(FIVE_PIXELS, FrameBlur.MAX_REPEATS + 1));
    }

    private static ByteBuffer streamOf(Consumer<SceneCommandWriter> commands) {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY));
        commands.accept(new SceneCommandWriter(stream));
        return stream.finish();
    }
}
