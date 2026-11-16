package io.github.luasgraphicss.minemod.codec;

import io.github.luasgraphicss.minemod.contract.abi.AbiVersion;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

public final class CommandStreamWriter {
    private final ByteBuffer stream;
    private int commandCount;
    private boolean finished;

    public CommandStreamWriter(ByteBuffer target) {
        this.stream = target.slice().order(ByteOrder.LITTLE_ENDIAN);
        if (stream.capacity() > CommandStreamFormat.MAX_STREAM_BYTES) {
            stream.limit(CommandStreamFormat.MAX_STREAM_BYTES);
        }
        reserve(CommandStreamFormat.STREAM_HEADER_BYTES);
        stream.position(CommandStreamFormat.STREAM_HEADER_BYTES);
    }

    public CommandStreamWriter createBuffer(ResourceHandle buffer, long sizeBytes, Set<BufferUsage> usages) {
        requireBuffer(buffer);
        if (sizeBytes < 1) {
            throw new IllegalArgumentException("buffer size must be positive: " + sizeBytes);
        }
        if (usages.isEmpty()) {
            throw new IllegalArgumentException("a buffer needs at least one usage");
        }
        beginCommand(CommandStreamFormat.OPCODE_CREATE_BUFFER, CommandStreamFormat.CREATE_BUFFER_BYTES);
        stream.putLong(buffer.bits());
        stream.putLong(sizeBytes);
        stream.putInt(BufferUsage.maskOf(usages));
        stream.putInt(0);
        return this;
    }

    public CommandStreamWriter writeBuffer(ResourceHandle buffer, long offset, ByteBuffer data) {
        requireBuffer(buffer);
        int dataBytes = data.remaining();
        if (dataBytes < 1) {
            throw new IllegalArgumentException("a buffer write needs at least one byte");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("buffer offset must not be negative: " + offset);
        }
        if (dataBytes > CommandStreamFormat.MAX_STREAM_BYTES) {
            throw new BufferOverflowException();
        }
        int paddedBytes = CommandStreamFormat.aligned(dataBytes);
        beginCommand(
                CommandStreamFormat.OPCODE_WRITE_BUFFER, CommandStreamFormat.WRITE_BUFFER_FIXED_BYTES + paddedBytes);
        stream.putLong(buffer.bits());
        stream.putLong(offset);
        stream.putLong(dataBytes);
        stream.put(data.duplicate());
        for (int padding = dataBytes; padding < paddedBytes; padding++) {
            stream.put((byte) 0);
        }
        return this;
    }

    public CommandStreamWriter destroyBuffer(ResourceHandle buffer) {
        requireBuffer(buffer);
        beginCommand(CommandStreamFormat.OPCODE_DESTROY_BUFFER, CommandStreamFormat.DESTROY_BUFFER_BYTES);
        stream.putLong(buffer.bits());
        return this;
    }

    public ByteBuffer appendRecord(short opcode, int payloadBytes) {
        if (payloadBytes < 0) {
            throw new IllegalArgumentException("a record payload cannot be negative: " + payloadBytes);
        }
        if (payloadBytes > CommandStreamFormat.MAX_STREAM_BYTES) {
            throw new BufferOverflowException();
        }
        int paddedBytes = CommandStreamFormat.aligned(payloadBytes);
        beginCommand(opcode, CommandStreamFormat.COMMAND_HEADER_BYTES + paddedBytes);
        ByteBuffer payload = stream.duplicate();
        payload.limit(payload.position() + paddedBytes);
        payload = payload.slice().order(ByteOrder.LITTLE_ENDIAN);
        for (int index = 0; index < paddedBytes; index++) {
            payload.put(index, (byte) 0);
        }
        stream.position(stream.position() + paddedBytes);
        return payload;
    }

    public ByteBuffer finish() {
        requireOpen();
        finished = true;
        int totalBytes = stream.position();
        stream.putInt(CommandStreamFormat.STREAM_MAGIC_OFFSET, CommandStreamFormat.MAGIC);
        stream.putShort(CommandStreamFormat.STREAM_MAJOR_OFFSET, (short) AbiVersion.CURRENT.major());
        stream.putShort(CommandStreamFormat.STREAM_MINOR_OFFSET, (short) AbiVersion.CURRENT.minor());
        stream.putInt(CommandStreamFormat.STREAM_COMMAND_COUNT_OFFSET, commandCount);
        stream.putInt(CommandStreamFormat.STREAM_TOTAL_BYTES_OFFSET, totalBytes);

        ByteBuffer encoded = stream.duplicate();
        encoded.position(0).limit(totalBytes);
        return encoded.slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    private void beginCommand(short opcode, int recordBytes) {
        requireOpen();
        if (commandCount == CommandStreamFormat.MAX_STREAM_COMMANDS) {
            throw new IllegalStateException(
                    "a command stream holds at most " + CommandStreamFormat.MAX_STREAM_COMMANDS + " commands");
        }
        reserve(recordBytes);
        stream.putShort(opcode);
        stream.putShort((short) 0);
        stream.putInt(recordBytes);
        commandCount++;
    }

    private void reserve(int bytes) {
        if (bytes > stream.remaining()) {
            throw new BufferOverflowException();
        }
    }

    private void requireOpen() {
        if (finished) {
            throw new IllegalStateException("the command stream is already finished");
        }
    }

    private static void requireBuffer(ResourceHandle handle) {
        if (handle.kind() != ResourceKind.BUFFER) {
            throw new IllegalArgumentException("expected a buffer handle, found " + handle);
        }
    }
}
