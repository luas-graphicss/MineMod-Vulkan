package io.github.luasgraphicss.minemod.codec;

final class CommandStreamFormat {
    static final int MAGIC = 0x53434D4D;
    static final int STREAM_HEADER_BYTES = 16;
    static final int MAX_STREAM_BYTES = 16 * 1024 * 1024;
    static final int MAX_STREAM_COMMANDS = 65536;

    static final int STREAM_MAGIC_OFFSET = 0;
    static final int STREAM_MAJOR_OFFSET = 4;
    static final int STREAM_MINOR_OFFSET = 6;
    static final int STREAM_COMMAND_COUNT_OFFSET = 8;
    static final int STREAM_TOTAL_BYTES_OFFSET = 12;

    static final int COMMAND_HEADER_BYTES = 8;
    static final int COMMAND_ALIGNMENT = 8;
    static final int COMMAND_OPCODE_OFFSET = 0;
    static final int COMMAND_FLAGS_OFFSET = 2;
    static final int COMMAND_BYTES_OFFSET = 4;

    static final short OPCODE_CREATE_BUFFER = 1;
    static final short OPCODE_WRITE_BUFFER = 2;
    static final short OPCODE_DESTROY_BUFFER = 3;

    static final int CREATE_BUFFER_BYTES = COMMAND_HEADER_BYTES + 24;
    static final int WRITE_BUFFER_FIXED_BYTES = COMMAND_HEADER_BYTES + 24;
    static final int DESTROY_BUFFER_BYTES = COMMAND_HEADER_BYTES + 8;

    private CommandStreamFormat() {}

    static int aligned(int bytes) {
        return (bytes + COMMAND_ALIGNMENT - 1) / COMMAND_ALIGNMENT * COMMAND_ALIGNMENT;
    }
}
