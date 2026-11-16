package io.github.luasgraphicss.minemod.compatibility.translation.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class OpenConstructsTest {
    private static final long[] NONE = new long[0];
    private static final int GL_QUADS = 0x0007;
    private static final int GL_COMPILE = 0x1300;
    private static final int GL_COMPILE_AND_EXECUTE = 0x1301;
    private static final int GL_PROJECTION = 0x1701;

    private final OpenConstructs constructs = new OpenConstructs();
    private final RecordingProvider provider = new RecordingProvider();

    @Test
    void aBalancedCallbackLeavesNothingOpen() {
        constructs.observe("glPushMatrix", NONE);
        constructs.observe("glBegin", new long[] {GL_QUADS});
        constructs.observe("glEnd", NONE);
        constructs.observe("glPopMatrix", NONE);

        assertEquals(Collections.emptyList(), constructs.closeSection(provider));
        assertEquals(Collections.emptyList(), provider.events());
    }

    @Test
    void anOpenPrimitiveAndAnOpenListAreClosedInTheProvider() {
        constructs.observe("glNewList", new long[] {4, GL_COMPILE});
        constructs.observe("glBegin", new long[] {GL_QUADS});

        assertEquals(
                Arrays.asList("a glBegin primitive was left open", "a display list was left open"),
                constructs.closeSection(provider));

        assertEquals(Arrays.asList("glEnd[]", "glEndList[]"), provider.events());
        assertFalse(constructs.isPrimitiveOpen());
        assertFalse(constructs.isCompilingList());
    }

    @Test
    void onlyACompileOnlyListDefersItsDraws() {
        constructs.observe("glNewList", new long[] {4, GL_COMPILE});
        assertTrue(constructs.defersExecution());

        constructs.observe("glEndList", NONE);
        constructs.observe("glNewList", new long[] {5, GL_COMPILE_AND_EXECUTE});

        assertFalse(constructs.defersExecution());
        assertTrue(constructs.isCompilingList());
    }

    @Test
    void matrixLevelsAreCountedPerStackSinceTheSectionBegan() {
        constructs.observe("glPushMatrix", NONE);
        constructs.beginSection();
        constructs.observe("glPushMatrix", NONE);
        constructs.observe("glPushMatrix", NONE);
        constructs.observe("glMatrixMode", new long[] {GL_PROJECTION});
        constructs.observe("glPopMatrix", NONE);

        assertEquals(
                Arrays.asList(
                        "matrix stack 0x1700 was left 2 level(s) from where it started",
                        "matrix stack 0x1701 was left -1 level(s) from where it started"),
                constructs.closeSection(provider));
    }
}
