package io.github.luasgraphicss.minemod.compatibility.state.reconcile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StateReconcilerTest {
    private static final int BLEND = 0;
    private static final int DEPTH_TEST = 1;
    private static final long OFF = 0;
    private static final long ON = 1;

    private final long[] providerState = {OFF, OFF};
    private final Map<Integer, Long> adopted = new HashMap<>();
    private int appliedCount;
    private final StateReconciler reconciler = new StateReconciler(
            new long[] {OFF, OFF},
            new LeftoverPolicy[] {LeftoverPolicy.ADOPT, LeftoverPolicy.RESTORE},
            this::applyToProvider,
            adopted::put);

    @Test
    void semanticChangesOutsideASectionStayOutOfTheContext() {
        reconciler.setSemantic(BLEND, ON);

        assertEquals(OFF, providerState[BLEND]);
        assertEquals(0, appliedCount);
    }

    @Test
    void enteringASectionAppliesOnlyWhatDiffers() {
        reconciler.setSemantic(BLEND, ON);

        assertEquals(1, reconciler.enterSection());
        assertEquals(ON, providerState[BLEND]);
        assertEquals(OFF, providerState[DEPTH_TEST]);
        assertTrue(reconciler.inSection());
    }

    @Test
    void semanticChangesInsideASectionReachTheContext() {
        reconciler.enterSection();

        reconciler.setSemantic(DEPTH_TEST, ON);

        assertEquals(ON, providerState[DEPTH_TEST]);
        assertEquals(ON, reconciler.contextValue(DEPTH_TEST));
    }

    @Test
    void aSemanticChangeTheContextAlreadyHoldsIsNotAppliedAgain() {
        reconciler.enterSection();
        changeDirectly(BLEND, ON);

        reconciler.setSemantic(BLEND, ON);

        assertEquals(0, appliedCount);
    }

    @Test
    void adoptsWhatACallbackLeftWhereTheSlotAllowsIt() {
        reconciler.enterSection();
        changeDirectly(BLEND, ON);

        assertEquals(1, reconciler.exitSection());
        assertEquals(ON, reconciler.semanticValue(BLEND));
        assertEquals(Collections.singletonMap(BLEND, ON), adopted);
        assertFalse(reconciler.inSection());
    }

    @Test
    void restoresASlotThatMustNotKeepLeftovers() {
        reconciler.enterSection();
        changeDirectly(DEPTH_TEST, ON);

        assertEquals(0, reconciler.exitSection());
        assertEquals(OFF, reconciler.semanticValue(DEPTH_TEST));
        assertEquals(1, reconciler.enterSection());
        assertEquals(OFF, providerState[DEPTH_TEST]);
    }

    @Test
    void aCallbackThatRestoresItsOwnStateLeavesNothingToReconcile() {
        reconciler.enterSection();
        changeDirectly(BLEND, ON);
        changeDirectly(BLEND, OFF);

        assertEquals(0, reconciler.exitSection());
        assertEquals(0, reconciler.enterSection());
        assertTrue(adopted.isEmpty());
    }

    @Test
    void rejectsNestedSectionsAndDirectChangesOutsideOne() {
        assertThrows(IllegalStateException.class, () -> reconciler.observeDirect(BLEND, ON));
        assertThrows(IllegalStateException.class, reconciler::exitSection);

        reconciler.enterSection();

        assertThrows(IllegalStateException.class, reconciler::enterSection);
    }

    @Test
    void rejectsPoliciesThatDoNotCoverEverySlot() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StateReconciler(
                        new long[] {OFF, OFF},
                        new LeftoverPolicy[] {LeftoverPolicy.ADOPT},
                        this::applyToProvider,
                        adopted::put));
    }

    private void applyToProvider(int slot, long value) {
        providerState[slot] = value;
        appliedCount++;
    }

    private void changeDirectly(int slot, long value) {
        providerState[slot] = value;
        reconciler.observeDirect(slot, value);
    }
}
