package io.github.luasgraphicss.minemod.compatibility.translation.call;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushLog;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.OpenConstructs;
import io.github.luasgraphicss.minemod.compatibility.translation.names.NameArguments;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CallTranslatorTest {
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_ARRAY_BUFFER = 0x8892;
    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_COMPILE = 0x1300;
    private static final int GL_INVALID_OPERATION = 0x0502;

    private final LayerFixture fixture = new LayerFixture();
    private final SubmissionCheckpoints checkpoints = new SubmissionCheckpoints(fixture.provider, new FlushLog(8));
    private final List<String> observed = new ArrayList<>();

    CallTranslatorTest() {
        checkpoints.beginFrame(1);
    }

    @Test
    void theProviderSeesItsOwnObjectInPlaceOfTheModsName() {
        ObjectNames names = fixture.names(ContextProfile.CORE);
        int texture = names.generate(ObjectKind.TEXTURE, 1)[0];

        translator(names).call("glBindTexture", new long[] {GL_TEXTURE_2D, texture});

        assertEquals(
                Collections.singletonList("glBindTexture[3553, " + RecordingProvider.FIRST_BACKING + "]"),
                fixture.provider.events());
    }

    @Test
    void aDrawTakesTheAttachmentsBeforeTheProviderDraws() {
        translator(fixture.names(ContextProfile.CORE)).call("glDrawArrays", new long[] {GL_TRIANGLES, 0, 3});

        assertEquals(Arrays.asList(RecordingProvider.RELEASE, "glDrawArrays[4, 0, 3]"), fixture.provider.events());
    }

    @Test
    void aReadWaitsForTheProviderBeforeReturningPixels() {
        translator(fixture.names(ContextProfile.CORE)).call("glReadPixels", new long[] {0, 0, 4, 4, 0x1908, 0x1401, 0});

        assertEquals(
                Arrays.asList(RecordingProvider.RELEASE, RecordingProvider.FINISH, "glReadPixels[0, 0, 4, 4, 6408, 5121, 0]"),
                fixture.provider.events());
    }

    @Test
    void drawsRecordedIntoADisplayListDoNotTakeTheAttachments() {
        CallTranslator translator = translator(fixture.names(ContextProfile.COMPATIBILITY));

        translator.call("glNewList", new long[] {1, GL_COMPILE});
        translator.call("glBegin", new long[] {GL_TRIANGLES});
        translator.call("glEnd", new long[0]);
        translator.call("glEndList", new long[0]);
        translator.call("glCallList", new long[] {1});

        assertEquals(
                Arrays.asList(
                        "glNewList[" + RecordingProvider.FIRST_BACKING + ", 4864]",
                        "glBegin[4]",
                        "glEnd[]",
                        "glEndList[]",
                        RecordingProvider.RELEASE,
                        "glCallList[" + RecordingProvider.FIRST_BACKING + "]"),
                fixture.provider.events());
    }

    @Test
    void aCallOnANameThatDoesNotExistIsDroppedWithItsError() {
        long result = translator(fixture.names(ContextProfile.CORE))
                .call("glBindVertexArray", new long[] {7});

        assertEquals(CallTranslator.DROPPED, result);
        assertEquals(Collections.emptyList(), fixture.provider.events());
        assertEquals(Collections.emptyList(), observed);
        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());
    }

    @Test
    void observersFollowTheCoreFunctionWithTheModsOwnNames() {
        ObjectNames names = fixture.names(ContextProfile.COMPATIBILITY);
        int buffer = names.generate(ObjectKind.BUFFER, 1)[0];

        translator(names).call("glBindBufferARB", new long[] {GL_ARRAY_BUFFER, buffer});

        assertEquals(Collections.singletonList("glBindBuffer[34962, " + buffer + "]"), observed);
        assertEquals(
                Collections.singletonList("glBindBufferARB[34962, " + RecordingProvider.FIRST_BACKING + "]"),
                fixture.provider.events());
    }

    @Test
    void functionsServedElsewhereAreNotTranslatedHere() {
        CallTranslator translator = translator(fixture.names(ContextProfile.CORE));

        assertThrows(IllegalArgumentException.class, () -> translator.call("glGetError", new long[0]));
        assertThrows(IllegalArgumentException.class, () -> translator.call("glUniform1f", new long[] {0, 0}));
        assertThrows(IllegalArgumentException.class, () -> translator.call("glProgramStringARB", new long[0]));
    }

    private CallTranslator translator(ObjectNames names) {
        CallObserver recorder = (function, arguments) -> observed.add(function + Arrays.toString(arguments));
        return new CallTranslator(
                CallCatalog.standard(),
                new NameArguments(names),
                checkpoints,
                new OpenConstructs(),
                Collections.singletonList(recorder),
                fixture.provider);
    }
}
