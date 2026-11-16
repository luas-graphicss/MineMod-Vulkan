package io.github.luasgraphicss.minemod.codec.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.luasgraphicss.minemod.codec.CommandStreamValidator;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class SceneRecordValidationTest {
    private static final int STREAM_CAPACITY = 1024;
    private static final int ALIGNMENT = 8;
    private static final int TEXEL_LENGTH_FIELD = SceneOpcode.WRITE_TEXTURE.fixedBytes() - Long.BYTES;

    @Test
    void acceptsARecordWithoutBlobOnlyAtItsExactSize() {
        for (SceneOpcode opcode : SceneOpcode.values()) {
            if (opcode.carriesBlob()) {
                continue;
            }
            assertEquals(AbiResult.OK, validate(opcode, opcode.fixedBytes()).result(), opcode.name());
            assertEquals(
                    AbiResult.LENGTH_OUT_OF_BOUNDS,
                    validate(opcode, opcode.fixedBytes() + ALIGNMENT).result(),
                    opcode.name());
        }
    }

    @Test
    void acceptsABlobThatFillsItsRecordUpToThePadding() {
        assertEquals(StreamReport.accepted(1), validate(textureWrite(5, 5)));
    }

    @Test
    void rejectsABlobDeclaredLongerThanItsRecord() {
        assertEquals(AbiResult.LENGTH_OUT_OF_BOUNDS, validate(textureWrite(64, 8)).result());
    }

    @Test
    void rejectsABlobThatLeavesAFullAlignmentUnitUnused() {
        assertEquals(AbiResult.LENGTH_OUT_OF_BOUNDS, validate(textureWrite(8, 16)).result());
    }

    @Test
    void rejectsABlobRecordShorterThanItsFixedFields() {
        SceneOpcode opcode = SceneOpcode.WRITE_TEXTURE;

        assertEquals(AbiResult.LENGTH_OUT_OF_BOUNDS, validate(opcode, opcode.fixedBytes() - ALIGNMENT).result());
    }

    @Test
    void treatsTheBlobLengthAsUnsigned() {
        assertEquals(AbiResult.LENGTH_OUT_OF_BOUNDS, validate(textureWrite(Long.MIN_VALUE, 8)).result());
    }

    @Test
    void reportsAnEmptyBlobAsMalformed() {
        assertEquals(AbiResult.MALFORMED_STREAM, validate(textureWrite(0, 0)).result());
    }

    @Test
    void knowsNoOpcodeAroundTheSceneCommands() {
        assertFalse(SceneOpcode.find((short) 15).isPresent());
        assertFalse(SceneOpcode.find((short) 32).isPresent());
        assertFalse(SceneOpcode.find((short) -1).isPresent());
    }

    private static StreamReport validate(SceneOpcode opcode, int payloadBytes) {
        CommandStreamWriter writer = new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY));
        writer.appendRecord(opcode.code(), payloadBytes);
        return CommandStreamValidator.validate(writer.finish());
    }

    private static ByteBuffer textureWrite(long declaredTexelBytes, int carriedBytes) {
        CommandStreamWriter writer = new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY));
        ByteBuffer payload = writer.appendRecord(
                SceneOpcode.WRITE_TEXTURE.code(), SceneOpcode.WRITE_TEXTURE.fixedBytes() + carriedBytes);
        payload.putLong(TEXEL_LENGTH_FIELD, declaredTexelBytes);
        return writer.finish();
    }

    private static StreamReport validate(ByteBuffer stream) {
        return CommandStreamValidator.validate(stream);
    }
}
