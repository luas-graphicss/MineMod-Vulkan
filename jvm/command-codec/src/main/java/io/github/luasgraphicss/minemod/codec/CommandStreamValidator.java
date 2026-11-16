package io.github.luasgraphicss.minemod.codec;

import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.abi.AbiVersion;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class CommandStreamValidator {
    private static final int UNSIGNED_SHORT_MASK = 0xFFFF;

    private CommandStreamValidator() {}

    public static StreamReport validate(ByteBuffer source) {
        ByteBuffer stream = source.slice().order(ByteOrder.LITTLE_ENDIAN);
        int streamBytes = stream.remaining();

        AbiResult header = checkStreamHeader(stream, streamBytes);
        if (header != AbiResult.OK) {
            return new StreamReport(header, 0, StreamReport.NO_COMMAND, 0);
        }

        int commandCount = stream.getInt(CommandStreamFormat.STREAM_COMMAND_COUNT_OFFSET);
        int cursor = CommandStreamFormat.STREAM_HEADER_BYTES;
        for (int index = 0; index < commandCount; index++) {
            AbiResult record = checkRecord(stream, cursor, streamBytes - cursor);
            if (record != AbiResult.OK) {
                return new StreamReport(record, index, index, cursor);
            }
            cursor += stream.getInt(cursor + CommandStreamFormat.COMMAND_BYTES_OFFSET);
        }

        if (cursor != streamBytes) {
            return new StreamReport(AbiResult.MALFORMED_STREAM, commandCount, StreamReport.NO_COMMAND, cursor);
        }
        return StreamReport.accepted(commandCount);
    }

    private static AbiResult checkStreamHeader(ByteBuffer stream, int streamBytes) {
        if (streamBytes > CommandStreamFormat.MAX_STREAM_BYTES) {
            return AbiResult.LIMIT_EXCEEDED;
        }
        if (streamBytes < CommandStreamFormat.STREAM_HEADER_BYTES) {
            return AbiResult.TRUNCATED_STREAM;
        }
        if (stream.getInt(CommandStreamFormat.STREAM_MAGIC_OFFSET) != CommandStreamFormat.MAGIC) {
            return AbiResult.MALFORMED_STREAM;
        }
        AbiVersion version = new AbiVersion(
                stream.getShort(CommandStreamFormat.STREAM_MAJOR_OFFSET) & UNSIGNED_SHORT_MASK,
                stream.getShort(CommandStreamFormat.STREAM_MINOR_OFFSET) & UNSIGNED_SHORT_MASK);
        if (!AbiVersion.CURRENT.accepts(version)) {
            return AbiResult.INCOMPATIBLE_ABI;
        }
        long totalBytes = Integer.toUnsignedLong(stream.getInt(CommandStreamFormat.STREAM_TOTAL_BYTES_OFFSET));
        if (totalBytes > streamBytes) {
            return AbiResult.TRUNCATED_STREAM;
        }
        if (totalBytes < streamBytes) {
            return AbiResult.MALFORMED_STREAM;
        }
        if (totalBytes % CommandStreamFormat.COMMAND_ALIGNMENT != 0) {
            return AbiResult.MISALIGNED;
        }
        long commandCount = Integer.toUnsignedLong(stream.getInt(CommandStreamFormat.STREAM_COMMAND_COUNT_OFFSET));
        if (commandCount > CommandStreamFormat.MAX_STREAM_COMMANDS) {
            return AbiResult.LIMIT_EXCEEDED;
        }
        return AbiResult.OK;
    }

    private static AbiResult checkRecord(ByteBuffer stream, int record, int remainingBytes) {
        if (remainingBytes < CommandStreamFormat.COMMAND_HEADER_BYTES) {
            return AbiResult.TRUNCATED_STREAM;
        }
        long declaredBytes = Integer.toUnsignedLong(stream.getInt(record + CommandStreamFormat.COMMAND_BYTES_OFFSET));
        if (declaredBytes < CommandStreamFormat.COMMAND_HEADER_BYTES || declaredBytes > remainingBytes) {
            return AbiResult.LENGTH_OUT_OF_BOUNDS;
        }
        if (declaredBytes % CommandStreamFormat.COMMAND_ALIGNMENT != 0) {
            return AbiResult.MISALIGNED;
        }
        if (stream.getShort(record + CommandStreamFormat.COMMAND_FLAGS_OFFSET) != 0) {
            return AbiResult.MALFORMED_STREAM;
        }

        int recordBytes = (int) declaredBytes;
        int payload = record + CommandStreamFormat.COMMAND_HEADER_BYTES;
        switch (stream.getShort(record + CommandStreamFormat.COMMAND_OPCODE_OFFSET)) {
            case CommandStreamFormat.OPCODE_CREATE_BUFFER:
                return checkCreateBuffer(stream, payload, recordBytes);
            case CommandStreamFormat.OPCODE_WRITE_BUFFER:
                return checkWriteBuffer(stream, payload, recordBytes);
            case CommandStreamFormat.OPCODE_DESTROY_BUFFER:
                return recordBytes == CommandStreamFormat.DESTROY_BUFFER_BYTES
                        ? AbiResult.OK
                        : AbiResult.LENGTH_OUT_OF_BOUNDS;
            default:
                return AbiResult.UNKNOWN_OPCODE;
        }
    }

    private static AbiResult checkCreateBuffer(ByteBuffer stream, int payload, int recordBytes) {
        if (recordBytes != CommandStreamFormat.CREATE_BUFFER_BYTES) {
            return AbiResult.LENGTH_OUT_OF_BOUNDS;
        }
        long sizeBytes = stream.getLong(payload + 8);
        int usage = stream.getInt(payload + 16);
        int reserved = stream.getInt(payload + 20);
        boolean knownUsage = usage != 0 && (usage & ~BufferUsage.knownMask()) == 0;
        return reserved == 0 && sizeBytes != 0 && knownUsage ? AbiResult.OK : AbiResult.MALFORMED_STREAM;
    }

    private static AbiResult checkWriteBuffer(ByteBuffer stream, int payload, int recordBytes) {
        if (recordBytes < CommandStreamFormat.WRITE_BUFFER_FIXED_BYTES) {
            return AbiResult.LENGTH_OUT_OF_BOUNDS;
        }
        long offset = stream.getLong(payload + 8);
        long dataBytes = stream.getLong(payload + 16);
        long dataCapacity = recordBytes - CommandStreamFormat.WRITE_BUFFER_FIXED_BYTES;
        boolean fitsRecord = Long.compareUnsigned(dataBytes, dataCapacity) <= 0;
        if (!fitsRecord || dataCapacity - dataBytes >= CommandStreamFormat.COMMAND_ALIGNMENT) {
            return AbiResult.LENGTH_OUT_OF_BOUNDS;
        }
        if (dataBytes == 0) {
            return AbiResult.MALFORMED_STREAM;
        }
        long lastOffsetBeforeOverflow = -1L - dataBytes;
        if (Long.compareUnsigned(offset, lastOffsetBeforeOverflow) > 0) {
            return AbiResult.ARITHMETIC_OVERFLOW;
        }
        return AbiResult.OK;
    }
}
