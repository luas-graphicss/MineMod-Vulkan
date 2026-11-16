package io.github.luasgraphicss.minemod.compatibility.state.reconcile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

class StateSequenceTest {
    private static final long BASE_SEED = 0x4D494E454D4F44L;
    private static final int SEQUENCES = 10_000;
    private static final int STEPS_PER_SEQUENCE = 48;
    private static final int SLOTS = 12;
    private static final int VALUES_PER_SLOT = 4;

    @Test
    void generatedSequencesKeepTheContextAndTheWrappersCoherent() {
        for (int sequence = 0; sequence < SEQUENCES; sequence++) {
            new GeneratedSequence(BASE_SEED + sequence).play();
        }
    }

    private static final class GeneratedSequence {
        private final long seed;
        private final Random random;
        private final long[] providerState = new long[SLOTS];
        private final long[] wrapperCache = new long[SLOTS];
        private final LeftoverPolicy[] policies = new LeftoverPolicy[SLOTS];
        private final StateReconciler reconciler;
        private int appliedCount;

        private GeneratedSequence(long seed) {
            this.seed = seed;
            this.random = new Random(seed);
            for (int slot = 0; slot < SLOTS; slot++) {
                policies[slot] = random.nextBoolean() ? LeftoverPolicy.ADOPT : LeftoverPolicy.RESTORE;
            }
            this.reconciler = new StateReconciler(
                    new long[SLOTS], policies, this::applyToProvider, (slot, value) -> wrapperCache[slot] = value);
        }

        private void play() {
            for (int step = 0; step < STEPS_PER_SEQUENCE; step++) {
                String where = "seed " + seed + ", step " + step;
                int choice = random.nextInt(4);
                if (choice == 0) {
                    toggleSection(where);
                } else if (choice == 1 && reconciler.inSection()) {
                    changeDirectly();
                } else {
                    changeThroughWrapper(where);
                }
            }
        }

        private void toggleSection(String where) {
            if (reconciler.inSection()) {
                exitSection(where);
            } else {
                enterSection(where);
            }
        }

        private void enterSection(String where) {
            int differing = 0;
            for (int slot = 0; slot < SLOTS; slot++) {
                if (providerState[slot] != wrapperCache[slot]) {
                    differing++;
                }
            }
            appliedCount = 0;

            assertEquals(differing, reconciler.enterSection(), where);
            assertEquals(differing, appliedCount, where);
            assertArrayEquals(wrapperCache, providerState, where);
        }

        private void exitSection(String where) {
            long[] expectedCache = wrapperCache.clone();
            for (int slot = 0; slot < SLOTS; slot++) {
                if (policies[slot] == LeftoverPolicy.ADOPT) {
                    expectedCache[slot] = providerState[slot];
                }
            }

            reconciler.exitSection();

            assertArrayEquals(expectedCache, wrapperCache, where);
            for (int slot = 0; slot < SLOTS; slot++) {
                assertEquals(wrapperCache[slot], reconciler.semanticValue(slot), where);
            }
        }

        private void changeDirectly() {
            int slot = random.nextInt(SLOTS);
            long value = random.nextInt(VALUES_PER_SLOT);
            providerState[slot] = value;
            reconciler.observeDirect(slot, value);
        }

        private void changeThroughWrapper(String where) {
            int slot = random.nextInt(SLOTS);
            long value = random.nextInt(VALUES_PER_SLOT);
            wrapperCache[slot] = value;
            reconciler.setSemantic(slot, value);
            if (reconciler.inSection()) {
                assertEquals(value, providerState[slot], where);
            }
        }

        private void applyToProvider(int slot, long value) {
            providerState[slot] = value;
            appliedCount++;
        }
    }
}
