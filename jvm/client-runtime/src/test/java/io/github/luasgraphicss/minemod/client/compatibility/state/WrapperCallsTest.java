package io.github.luasgraphicss.minemod.client.compatibility.state;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class WrapperCallsTest {
    private static final long GL_LEQUAL = 0x0203;
    private static final long GL_ALWAYS = 0x0207;

    private final WrapperCalls calls = new WrapperCalls();
    private final List<String> functions = new ArrayList<>();
    private final List<long[]> words = new ArrayList<>();

    @Test
    void aKeptCallIsReplayedOnceWithItsArguments() {
        calls.keep(WrapperCall.VIEWPORT, 0, 0, 854, 480);

        assertEquals(1, replay());
        assertEquals(0, replay());
        assertEquals(Collections.singletonList("glViewport"), functions);
        assertArrayEquals(new long[] {0, 0, 854, 480}, words.get(0));
    }

    @Test
    void onlyTheLastArgumentsOfACallAreReplayed() {
        calls.keep(WrapperCall.DEPTH_FUNCTION, GL_ALWAYS);
        calls.keep(WrapperCall.DEPTH_FUNCTION, GL_LEQUAL);

        replay();

        assertEquals(Collections.singletonList("glDepthFunc"), functions);
        assertArrayEquals(new long[] {GL_LEQUAL}, words.get(0));
    }

    @Test
    void aCallKeptAgainWithTheSameArgumentsIsReplayedAgain() {
        calls.keep(WrapperCall.DEPTH_MASK, WrapperCalls.word(true));
        replay();
        calls.keep(WrapperCall.DEPTH_MASK, WrapperCalls.word(true));

        assertEquals(1, replay());
    }

    @Test
    void severalCallsAreReplayedInOneOrder() {
        calls.keep(WrapperCall.SCISSOR_BOX, 1, 2, 3, 4);
        calls.keep(WrapperCall.DEPTH_OFFSET, WrapperCalls.word(-1.0f), WrapperCalls.word(-10.0f));
        calls.keep(WrapperCall.VIEWPORT, 0, 0, 8, 8);

        replay();

        assertEquals(Arrays.asList("glViewport", "glScissor", "glPolygonOffset"), functions);
        assertArrayEquals(new long[] {0xBF800000L, 0xC1200000L}, words.get(2));
    }

    @Test
    void theProviderKeepsArgumentsALaterCallDoesNotChange() {
        calls.keep(WrapperCall.COLOR_MASK, 1, 1, 1, 0);
        replay();
        calls.keep(WrapperCall.COLOR_MASK, 0, 0, 0, 0);

        assertArrayEquals(new long[] {1, 1, 1, 0}, words.get(0));
    }

    @Test
    void aCallIsRefusedWithTheWrongNumberOfArguments() {
        assertThrows(IllegalArgumentException.class, () -> calls.keep(WrapperCall.VIEWPORT, 1));
        assertThrows(IllegalArgumentException.class, () -> calls.keep(WrapperCall.DEPTH_FUNCTION, 1, 2));
        assertEquals(0, replay());
    }

    private int replay() {
        return calls.replayInto((function, arguments) -> {
            functions.add(function);
            words.add(arguments);
        });
    }
}
