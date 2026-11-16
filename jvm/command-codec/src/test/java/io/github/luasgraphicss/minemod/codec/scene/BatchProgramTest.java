package io.github.luasgraphicss.minemod.codec.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class BatchProgramTest {
    private static final int STREAM_CAPACITY = 1024;
    private static final int STREAM_HEADER_BYTES = 16;
    private static final int COMMAND_HEADER_BYTES = 8;
    private static final int FIELDS_BEFORE_THE_PROGRAM = 9;
    private static final int PROGRAM_OFFSET =
            STREAM_HEADER_BYTES + COMMAND_HEADER_BYTES + FIELDS_BEFORE_THE_PROGRAM * Integer.BYTES;
    private static final int POSITION_BYTES = 3 * Float.BYTES;
    private static final int STAGED_PROGRAM = 5;

    @Test
    void drawsWithTheBuiltInProgramUnlessOneIsNamed() {
        assertEquals(BatchState.BUILT_IN_PROGRAM, programIn(streamOf(triangle().build())));
    }

    @Test
    void writesTheStagedProgramWhereTheRendererReadsIt() {
        assertEquals(STAGED_PROGRAM, programIn(streamOf(triangle().program(STAGED_PROGRAM).build())));
    }

    @Test
    void refusesAProgramBelowTheBuiltInOne() {
        assertThrows(IllegalArgumentException.class, () -> triangle().program(-1));
    }

    private static BatchState.Builder triangle() {
        return BatchState.builder(BatchSlot.SOLID, 0, EnumSet.of(VertexElement.POSITION), PrimitiveMode.TRIANGLES);
    }

    private static ByteBuffer streamOf(BatchState batch) {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY));
        new SceneCommandWriter(stream).drawBatch(batch, ByteBuffer.allocate(3 * POSITION_BYTES));
        return stream.finish();
    }

    private static int programIn(ByteBuffer stream) {
        return stream.duplicate().order(ByteOrder.LITTLE_ENDIAN).getInt(stream.position() + PROGRAM_OFFSET);
    }
}
