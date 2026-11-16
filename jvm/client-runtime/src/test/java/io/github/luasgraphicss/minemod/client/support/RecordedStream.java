package io.github.luasgraphicss.minemod.client.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.codec.CommandStreamValidator;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public final class RecordedStream {
    private static final int CAPACITY_BYTES = 64 * 1024;
    private static final int STREAM_HEADER_BYTES = 16;
    private static final int RECORD_HEADER_BYTES = 8;
    private static final int RECORD_BYTES_OFFSET = 4;

    private final CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocateDirect(CAPACITY_BYTES));
    private final SceneCommandWriter scene = new SceneCommandWriter(stream);
    private final List<SceneOpcode> opcodes = new ArrayList<>();
    private final List<ByteBuffer> payloads = new ArrayList<>();

    public SceneCommandWriter scene() {
        return scene;
    }

    public List<SceneOpcode> opcodes() {
        ByteBuffer encoded = stream.finish();
        StreamReport report = CommandStreamValidator.validate(encoded);
        assertTrue(report.isAccepted(), "the recorded stream is well formed: " + report);
        for (int record = STREAM_HEADER_BYTES; record < encoded.limit(); ) {
            int recordBytes = encoded.getInt(record + RECORD_BYTES_OFFSET);
            opcodes.add(SceneOpcode.find(encoded.getShort(record)).get());
            ByteBuffer payload = encoded.duplicate();
            payload.limit(record + recordBytes).position(record + RECORD_HEADER_BYTES);
            payloads.add(payload.slice().order(ByteOrder.LITTLE_ENDIAN));
            record += recordBytes;
        }
        return opcodes;
    }

    public ByteBuffer payload(int record) {
        return payloads.get(record).duplicate().order(ByteOrder.LITTLE_ENDIAN);
    }
}
