package io.github.luasgraphicss.minemod.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommandStreamMutationTest {
    private static final int GENERATED_STREAMS = 300;
    private static final int MUTATIONS_PER_STREAM = 40;
    private static final Set<AbiResult> STRUCTURAL_RESULTS = EnumSet.of(
            AbiResult.OK,
            AbiResult.INCOMPATIBLE_ABI,
            AbiResult.TRUNCATED_STREAM,
            AbiResult.MALFORMED_STREAM,
            AbiResult.UNKNOWN_OPCODE,
            AbiResult.LENGTH_OUT_OF_BOUNDS,
            AbiResult.MISALIGNED,
            AbiResult.ARITHMETIC_OVERFLOW,
            AbiResult.LIMIT_EXCEEDED);

    @Test
    void everyGeneratedStreamIsAccepted() {
        CommandSequenceGenerator generator = new CommandSequenceGenerator(new Random(0x4D494E454D4F4401L));

        for (int round = 0; round < GENERATED_STREAMS; round++) {
            CommandSequenceGenerator.Sequence sequence = generator.next();

            assertEquals(StreamReport.accepted(sequence.commandCount), CommandStreamValidator.validate(sequence.stream));
        }
    }

    @Test
    void mutatedStreamsEndInAStructuralResultInsideTheStream() {
        Random random = new Random(0x4D494E454D4F4403L);
        CommandSequenceGenerator generator = new CommandSequenceGenerator(new Random(0x4D494E454D4F4402L));

        for (int round = 0; round < GENERATED_STREAMS; round++) {
            ByteBuffer stream = generator.next().stream;
            for (int mutation = 0; mutation < MUTATIONS_PER_STREAM; mutation++) {
                ByteBuffer mutated = CommandSequenceGenerator.mutate(stream, random);

                StreamReport report = CommandStreamValidator.validate(mutated);

                assertTrue(STRUCTURAL_RESULTS.contains(report.result()), report.toString());
                assertTrue(report.isAccepted() || report.failedOffset() <= mutated.remaining(), report.toString());
            }
        }
    }
}
