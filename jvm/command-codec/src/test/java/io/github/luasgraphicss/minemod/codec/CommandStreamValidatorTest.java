package io.github.luasgraphicss.minemod.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class CommandStreamValidatorTest {
    private static final ResourceHandle BUFFER = ResourceHandle.of(ResourceKind.BUFFER, 1, 0);
    private static final int CREATE_RECORD = 16;
    private static final int WRITE_RECORD = CREATE_RECORD + 32;
    private static final int WRITE_OFFSET_FIELD = WRITE_RECORD + 16;
    private static final int WRITE_DATA_BYTES_FIELD = WRITE_RECORD + 24;
    private static final int COMMAND_COUNT_FIELD = 8;

    @Test
    void reportsTheCommandAndOffsetWhereAStreamFails() {
        ByteBuffer stream = lifecycle();
        stream.putLong(WRITE_OFFSET_FIELD, -3L);

        assertEquals(
                new StreamReport(AbiResult.ARITHMETIC_OVERFLOW, 1, 1, WRITE_RECORD),
                CommandStreamValidator.validate(stream));
    }

    @Test
    void treatsLengthFieldsAsUnsigned() {
        ByteBuffer stream = lifecycle();
        stream.putLong(WRITE_DATA_BYTES_FIELD, Long.MIN_VALUE);

        assertEquals(AbiResult.LENGTH_OUT_OF_BOUNDS, CommandStreamValidator.validate(stream).result());
    }

    @Test
    void treatsTheCommandCountAsUnsigned() {
        ByteBuffer stream = lifecycle();
        stream.putInt(COMMAND_COUNT_FIELD, -1);

        assertEquals(
                new StreamReport(AbiResult.LIMIT_EXCEEDED, 0, StreamReport.NO_COMMAND, 0),
                CommandStreamValidator.validate(stream));
    }

    @Test
    void reportsBytesLeftAfterTheDeclaredCommands() {
        ByteBuffer stream = lifecycle();
        stream.putInt(COMMAND_COUNT_FIELD, 1);

        assertEquals(
                new StreamReport(AbiResult.MALFORMED_STREAM, 1, StreamReport.NO_COMMAND, WRITE_RECORD),
                CommandStreamValidator.validate(stream));
    }

    @Test
    void validatesFromThePositionOfTheGivenBuffer() {
        ByteBuffer padded = ByteBuffer.allocate(256);
        padded.position(40);
        padded.put(lifecycle());
        padded.flip().position(40);

        assertEquals(StreamReport.accepted(3), CommandStreamValidator.validate(padded));
    }

    private static ByteBuffer lifecycle() {
        return new CommandStreamWriter(ByteBuffer.allocate(256))
                .createBuffer(BUFFER, 16, EnumSet.of(BufferUsage.VERTEX))
                .writeBuffer(BUFFER, 4, ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5}))
                .destroyBuffer(BUFFER)
                .finish()
                .order(ByteOrder.LITTLE_ENDIAN);
    }
}
