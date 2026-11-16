package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.codec.CommandSequenceGenerator;
import io.github.luasgraphicss.minemod.codec.CommandStreamCorpus;
import io.github.luasgraphicss.minemod.codec.CommandStreamValidator;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NativeValidatorAgreementTest {
    private static final int GENERATED_STREAMS = 200;
    private static final int MUTATIONS_PER_STREAM = 30;
    private static final int LEADING_BYTES = 24;
    private static final int TRAILING_BYTES = 8;

    @Test
    void givesEveryCorpusStreamItsRecordedResult() throws IOException {
        for (CommandStreamCorpus.Entry entry : CommandStreamCorpus.load()) {
            StreamReport report = LoadedBridge.BRIDGE.validate(DirectStreams.copyOf(entry.stream()));

            assertEquals(entry.expected, report.result(), entry.name);
        }
    }

    @Test
    void reportsGeneratedAndMutatedStreamsExactlyLikeTheJavaValidator() {
        Random random = new Random(0x4D494E454D4F4405L);
        CommandSequenceGenerator generator = new CommandSequenceGenerator(new Random(0x4D494E454D4F4404L));

        for (int round = 0; round < GENERATED_STREAMS; round++) {
            ByteBuffer stream = generator.next().stream;
            assertSameReport(stream);
            for (int mutation = 0; mutation < MUTATIONS_PER_STREAM; mutation++) {
                assertSameReport(CommandSequenceGenerator.mutate(stream, random));
            }
        }
    }

    @Test
    void validatesOnlyTheRemainingBytesOfTheBuffer() {
        CommandSequenceGenerator.Sequence sequence = new CommandSequenceGenerator(new Random(0x4D494E454D4F4406L)).next();
        int streamBytes = sequence.stream.remaining();
        ByteBuffer surrounded = ByteBuffer.allocateDirect(LEADING_BYTES + streamBytes + TRAILING_BYTES);
        surrounded.position(LEADING_BYTES);
        surrounded.put(sequence.stream.duplicate());
        surrounded.flip().position(LEADING_BYTES);

        assertEquals(StreamReport.accepted(sequence.commandCount), LoadedBridge.BRIDGE.validate(surrounded));
    }

    private static void assertSameReport(ByteBuffer stream) {
        assertEquals(
                CommandStreamValidator.validate(stream), LoadedBridge.BRIDGE.validate(DirectStreams.copyOf(stream)));
    }
}
