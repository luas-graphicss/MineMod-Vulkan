package io.github.luasgraphicss.minemod.codec.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.luasgraphicss.minemod.codec.CommandStreamValidator;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class SectionFaceCommandsTest {
    private static final int STREAM_CAPACITY = 128;
    private static final int FIRST_RECORD = 16;
    private static final int RECORD_HEADER_BYTES = 8;
    private static final int RECORD_BYTES_FIELD = FIRST_RECORD + 4;
    private static final int FIRST_PAYLOAD = FIRST_RECORD + RECORD_HEADER_BYTES;
    private static final long WEST_TO_EAST = 1L << 1;
    private static final long EAST_TO_WEST = 1L << 6;
    private static final long EVERY_PAIR = (1L << 36) - 1;

    @Test
    void connectsTwoFacesInBothDirections() {
        FaceConnections connections = FaceConnections.none().connecting(SectionFace.WEST, SectionFace.EAST);

        assertEquals(WEST_TO_EAST | EAST_TO_WEST, connections.pairs());
    }

    @Test
    void connectsNothingOrEveryPairOfFaces() {
        assertEquals(0, FaceConnections.none().pairs());
        assertEquals(EVERY_PAIR, FaceConnections.open().pairs());
    }

    @Test
    void comparesByTheConnectedPairs() {
        FaceConnections everyPair = FaceConnections.none();
        for (SectionFace first : SectionFace.values()) {
            for (SectionFace second : SectionFace.values()) {
                everyPair = everyPair.connecting(first, second);
            }
        }

        assertEquals(FaceConnections.open(), everyPair);
        assertEquals(FaceConnections.open().hashCode(), everyPair.hashCode());
        assertNotEquals(FaceConnections.open(), FaceConnections.none().connecting(SectionFace.WEST, SectionFace.EAST));
    }

    @Test
    void writesTheSectionItsWorldAndItsConnectedPairs() {
        ByteBuffer stream = facesOfSection(FaceConnections.none().connecting(SectionFace.DOWN, SectionFace.UP));

        assertEquals(31, stream.getShort(FIRST_RECORD));
        assertEquals(RECORD_HEADER_BYTES + 24, stream.getInt(RECORD_BYTES_FIELD));
        assertEquals(-2, stream.getInt(FIRST_PAYLOAD));
        assertEquals(4, stream.getInt(FIRST_PAYLOAD + 4));
        assertEquals(7, stream.getInt(FIRST_PAYLOAD + 8));
        assertEquals(3, stream.getInt(FIRST_PAYLOAD + 12));
        assertEquals((1L << 15) | (1L << 20), stream.getLong(FIRST_PAYLOAD + 16));
    }

    @Test
    void writesFacesTheValidatorAccepts() {
        assertEquals(StreamReport.accepted(1), CommandStreamValidator.validate(facesOfSection(FaceConnections.open())));
    }

    private static ByteBuffer facesOfSection(FaceConnections connections) {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY));
        new SceneCommandWriter(stream).connectSectionFaces(-2, 4, 7, 3, connections);
        return stream.finish();
    }
}
