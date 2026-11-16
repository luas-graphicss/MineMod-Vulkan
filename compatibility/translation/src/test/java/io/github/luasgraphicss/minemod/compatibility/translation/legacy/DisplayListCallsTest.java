package io.github.luasgraphicss.minemod.compatibility.translation.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushLog;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DisplayListCallsTest {
    private static final int GL_COMPILE = 0x1300;

    private final LayerFixture fixture = new LayerFixture();
    private final SubmissionCheckpoints checkpoints = new SubmissionCheckpoints(fixture.provider, new FlushLog(8));
    private final OpenConstructs constructs = new OpenConstructs();
    private final DisplayListNames lists =
            new DisplayListNames(() -> fixture.contextNames, fixture.provider, fixture.errors, () -> fixture.frame);
    private final DisplayListCalls calls = new DisplayListCalls(lists, constructs, checkpoints, fixture.provider);

    DisplayListCallsTest() {
        checkpoints.beginFrame(1);
    }

    @Test
    void calledListsReachTheProviderAsItsOwnListsAfterTheHandover() {
        lists.setBase(lists.generate(3));

        calls.callLists(new int[] {2, 0});

        assertEquals(
                Arrays.asList(
                        RecordingProvider.RELEASE,
                        "glCallLists[" + (RecordingProvider.FIRST_BACKING + 2) + ", " + RecordingProvider.FIRST_BACKING + "]"),
                fixture.provider.events());
    }

    @Test
    void listsCalledWhileCompilingAnotherListDoNotDrawYet() {
        lists.setBase(lists.generate(2));
        constructs.observe("glNewList", new long[] {2, GL_COMPILE});

        calls.callLists(new int[] {0});

        assertEquals(
                Collections.singletonList("glCallLists[" + RecordingProvider.FIRST_BACKING + "]"),
                fixture.provider.events());
    }
}
