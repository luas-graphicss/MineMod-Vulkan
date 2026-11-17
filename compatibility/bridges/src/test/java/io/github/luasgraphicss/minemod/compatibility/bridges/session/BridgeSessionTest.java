package io.github.luasgraphicss.minemod.compatibility.bridges.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.bridges.registry.BridgeRegistry;
import io.github.luasgraphicss.minemod.compatibility.bridges.support.RecordingBridge;
import io.github.luasgraphicss.minemod.compatibility.spi.SceneAttachments;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BridgeSessionTest {
    private static final String QUERY_RESULT = "query result across native draws";
    private static final String FEEDBACK = "transform feedback across native draws";
    private static final String FLUID_SURFACES = "fluid surfaces in the translucent pass";

    private final List<String> journal = new ArrayList<>();
    private final BridgeRegistry registry = new BridgeRegistry();
    private final RecordingBridge occlusion = new RecordingBridge("occlusion", journal, QUERY_RESULT);
    private final RecordingBridge feedback = new RecordingBridge("feedback", journal, FEEDBACK);
    private final BridgeHost host = new BridgeHost() {
        @Override
        public SceneAttachments attachments() {
            return new SceneAttachments(854, 480, 1);
        }

        @Override
        public void submit(String bridgeId, ByteBuffer commandStream) {
            journal.add(bridgeId + " submits " + commandStream.remaining() + " byte(s), read-only "
                    + commandStream.isReadOnly());
        }
    };
    private final BridgeSession session = new BridgeSession(registry, host);

    BridgeSessionTest() {
        registry.register(occlusion);
        registry.register(feedback);
    }

    @Test
    void anAttachedBridgeDrawsThroughTheHostUnderItsOwnId() {
        session.attachAll();

        occlusion.context().submit(ByteBuffer.allocate(24));

        assertEquals(854, occlusion.context().attachments().width());
        assertEquals("occlusion submits 24 byte(s), read-only true", journal.get(journal.size() - 1));
    }

    @Test
    void anOperationGoesToTheBridgeThatClaimedIt() {
        session.attachAll();

        assertEquals(2, session.serve(FEEDBACK, new long[] {7, 9}));

        assertEquals("feedback serves " + FEEDBACK + "[7, 9]", journal.get(journal.size() - 1));
        assertTrue(session.serves(QUERY_RESULT));
        assertFalse(session.serves("glReadBuffer on the front buffer"));
        assertThrows(
                IllegalStateException.class, () -> session.serve("glReadBuffer on the front buffer", new long[0]));
    }

    @Test
    void aDetachedBridgeCanNoLongerReachTheRenderer() {
        session.attachAll();

        assertEquals(Collections.emptyList(), session.detachAll());

        assertThrows(IllegalStateException.class, () -> occlusion.context().submit(ByteBuffer.allocate(8)));
        assertThrows(IllegalStateException.class, () -> feedback.context().attachments());
        assertFalse(session.serves(QUERY_RESULT));
        assertEquals(Arrays.asList("feedback detached", "occlusion detached"), journal.subList(2, 4));
    }

    @Test
    void oneFailingDetachDoesNotKeepTheOthersAttached() {
        session.attachAll();
        feedback.failDetach();

        assertEquals(Collections.singletonList("feedback: resources were still in use"), session.detachAll());

        assertThrows(IllegalStateException.class, () -> feedback.context().attachments());
        assertThrows(IllegalStateException.class, () -> occlusion.context().attachments());
    }

    @Test
    void attachingTwiceAttachesEachBridgeOnce() {
        session.attachAll();
        session.attachAll();

        assertEquals(Arrays.asList("occlusion attached", "feedback attached"), journal);
    }

    @Test
    void theBridgesOfOnePassAreServedInTheOrderOfThePass() {
        session.attachAll();
        journal.clear();

        List<String> failures = session.servePass(
                Arrays.asList(FEEDBACK, "an operation no bridge serves", QUERY_RESULT), new long[] {7});

        assertTrue(failures.isEmpty());
        assertEquals(
                Arrays.asList(
                        "feedback serves " + FEEDBACK + "[7]", "occlusion serves " + QUERY_RESULT + "[7]"),
                journal);
    }

    @Test
    void aBridgeThatFailsDoesNotStopTheOthersOfItsPass() {
        registry.register(new RecordingBridge("fluids", journal, FLUID_SURFACES) {
            @Override
            public long serve(String operation, long[] arguments) {
                throw new IllegalStateException("the fluid mesh was released");
            }
        });
        session.attachAll();
        journal.clear();

        List<String> failures = session.servePass(Arrays.asList(FLUID_SURFACES, QUERY_RESULT), new long[0]);

        assertEquals(
                Collections.singletonList("fluids in \"" + FLUID_SURFACES + "\": the fluid mesh was released"), failures);
        assertEquals(Collections.singletonList("occlusion serves " + QUERY_RESULT + "[]"), journal);
    }
}
