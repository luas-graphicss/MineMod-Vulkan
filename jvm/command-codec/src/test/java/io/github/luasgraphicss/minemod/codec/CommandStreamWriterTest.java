package io.github.luasgraphicss.minemod.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class CommandStreamWriterTest {
    private static final ResourceHandle BUFFER = ResourceHandle.of(ResourceKind.BUFFER, 1, 0);
    private static final byte[] DATA = {1, 2, 3, 4, 5};

    @Test
    void reproducesTheBufferLifecycleStreamOfTheCorpusByteForByte() throws IOException {
        ByteBuffer encoded = new CommandStreamWriter(ByteBuffer.allocateDirect(256))
                .createBuffer(BUFFER, 16, EnumSet.of(BufferUsage.VERTEX))
                .writeBuffer(BUFFER, 4, ByteBuffer.wrap(DATA))
                .destroyBuffer(BUFFER)
                .finish();

        assertArrayEquals(CommandStreamCorpus.named("valid-buffer-lifecycle.stream").bytes, bytesOf(encoded));
    }

    @Test
    void anEmptyStreamIsOnlyTheHeader() throws IOException {
        ByteBuffer encoded = new CommandStreamWriter(ByteBuffer.allocate(64)).finish();

        assertArrayEquals(CommandStreamCorpus.named("valid-empty.stream").bytes, bytesOf(encoded));
    }

    @Test
    void writesAtThePositionOfTheTargetAndLeavesTheDataBufferUntouched() {
        ByteBuffer target = ByteBuffer.allocate(128);
        target.position(24);
        ByteBuffer data = ByteBuffer.wrap(DATA);

        ByteBuffer encoded = new CommandStreamWriter(target).writeBuffer(BUFFER, 0, data).finish();

        assertEquals(StreamReport.accepted(1), CommandStreamValidator.validate(encoded));
        assertEquals(DATA.length, data.remaining());
        assertEquals(24, target.position());
    }

    @Test
    void failsInsteadOfWritingPastTheTarget() {
        CommandStreamWriter writer = new CommandStreamWriter(ByteBuffer.allocate(40));

        assertThrows(BufferOverflowException.class, () -> writer.createBuffer(BUFFER, 16, EnumSet.of(BufferUsage.INDEX)));
        assertThrows(BufferOverflowException.class, () -> new CommandStreamWriter(ByteBuffer.allocate(8)));
    }

    @Test
    void rejectsCommandsTheNativeValidatorWouldCallMalformed() {
        CommandStreamWriter writer = new CommandStreamWriter(ByteBuffer.allocate(256));

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.createBuffer(BUFFER, 0, EnumSet.of(BufferUsage.VERTEX)));
        assertThrows(
                IllegalArgumentException.class,
                () -> writer.createBuffer(BUFFER, 16, EnumSet.noneOf(BufferUsage.class)));
        assertThrows(IllegalArgumentException.class, () -> writer.writeBuffer(BUFFER, 0, ByteBuffer.allocate(0)));
        assertThrows(IllegalArgumentException.class, () -> writer.writeBuffer(BUFFER, -1, ByteBuffer.wrap(DATA)));
    }

    @Test
    void cannotBeReusedAfterFinishing() {
        CommandStreamWriter writer = new CommandStreamWriter(ByteBuffer.allocate(64));
        writer.finish();

        assertThrows(IllegalStateException.class, () -> writer.destroyBuffer(BUFFER));
        assertThrows(IllegalStateException.class, writer::finish);
    }

    private static byte[] bytesOf(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }
}
