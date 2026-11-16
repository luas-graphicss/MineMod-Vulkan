package io.github.luasgraphicss.minemod.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandStreamCorpusTest {
    @Test
    void everyCorpusStreamValidatesToItsRecordedResult() throws IOException {
        List<CommandStreamCorpus.Entry> corpus = CommandStreamCorpus.load();
        assertTrue(corpus.size() >= 16, "the corpus lost streams: " + corpus.size());

        for (CommandStreamCorpus.Entry entry : corpus) {
            assertEquals(entry.expected, CommandStreamValidator.validate(entry.stream()).result(), entry.name);
        }
    }
}
