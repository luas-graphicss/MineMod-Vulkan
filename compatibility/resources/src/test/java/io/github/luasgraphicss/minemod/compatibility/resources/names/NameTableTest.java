package io.github.luasgraphicss.minemod.compatibility.resources.names;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class NameTableTest {
    private final List<Integer> disposed = new ArrayList<>();
    private final ObjectDisposer disposer = (kind, backing) -> disposed.add(backing);
    private final NameTable textures = new NameTable(ObjectKind.TEXTURE, 4);

    @Test
    void allocatesTheLowestUnusedName() {
        int first = textures.allocate(101);
        int second = textures.allocate(102);
        textures.delete(first, disposer);

        assertEquals(1, first);
        assertEquals(2, second);
        assertEquals(1, textures.allocate(103));
    }

    @Test
    void translatesNamesInBothDirections() {
        int name = textures.allocate(101);

        assertEquals(101, textures.backingOf(name));
        assertEquals(OptionalInt.of(name), textures.nameOf(101));
        assertEquals(OptionalInt.empty(), textures.nameOf(999));
    }

    @Test
    void claimsANameChosenByTheCaller() {
        assertTrue(textures.claim(2, 200));

        assertTrue(textures.contains(2));
        assertEquals(1, textures.allocate(101));
        assertEquals(3, textures.allocate(103));
    }

    @Test
    void refusesToClaimANameInUseOrOutOfRange() {
        int name = textures.allocate(101);

        assertFalse(textures.claim(name, 201));
        assertFalse(textures.claim(0, 202));
        assertFalse(textures.claim(5, 203));
        assertEquals(1, textures.liveCount());
    }

    @Test
    void disposesADeletedObjectAtOnceWhenNoFrameUsedIt() {
        int name = textures.allocate(101);

        textures.delete(name, disposer);

        assertEquals(Collections.singletonList(101), disposed);
        assertEquals(0, textures.retiringCount());
    }

    @Test
    void keepsADeletedObjectUntilTheLastFrameThatUsedItCompletes() {
        int name = textures.allocate(101);
        textures.markUsed(name, 7);
        textures.markUsed(name, 5);

        textures.delete(name, disposer);

        assertFalse(textures.contains(name));
        assertEquals(0, textures.collect(6, disposer));
        assertEquals(Collections.emptyList(), disposed);
        assertEquals(1, textures.collect(7, disposer));
        assertEquals(Collections.singletonList(101), disposed);
        assertEquals(0, textures.retiringCount());
    }

    @Test
    void disposesADeletedObjectOfOneContextAtOnceEvenWhenAFrameUsedIt() {
        NameTable vertexArrays = new NameTable(ObjectKind.VERTEX_ARRAY, 4);
        int name = vertexArrays.allocate(101);
        vertexArrays.markUsed(name, 7);

        vertexArrays.delete(name, disposer);

        assertEquals(Collections.singletonList(101), disposed);
        assertEquals(0, vertexArrays.retiringCount());
    }

    @Test
    void reusesADeletedNameWhileItsOldObjectStillWaits() {
        int name = textures.allocate(101);
        textures.markUsed(name, 3);
        textures.delete(name, disposer);

        int reused = textures.allocate(102);

        assertEquals(name, reused);
        assertEquals(102, textures.backingOf(reused));
        assertEquals(1, textures.retiringCount());
    }

    @Test
    void rebindingKeepsTheNameAndRetiresTheReplacedBacking() {
        int name = textures.allocate(101);
        textures.markUsed(name, 4);

        textures.rebind(name, 102, disposer);

        assertEquals(102, textures.backingOf(name));
        assertEquals(OptionalInt.empty(), textures.nameOf(101));
        assertEquals(Collections.emptyList(), disposed);
        textures.collect(4, disposer);
        assertEquals(Collections.singletonList(101), disposed);
    }

    @Test
    void aReboundNameStartsWithoutFrameUses() {
        int name = textures.allocate(101);
        textures.markUsed(name, 4);
        textures.rebind(name, 102, disposer);

        textures.delete(name, disposer);

        assertEquals(Collections.singletonList(102), disposed);
    }

    @Test
    void rejectsABackingThatAlreadyHasAName() {
        int name = textures.allocate(101);

        assertThrows(IllegalArgumentException.class, () -> textures.allocate(101));
        assertThrows(IllegalArgumentException.class, () -> textures.rebind(name, 101, disposer));
    }

    @Test
    void rejectsNamesThatAreNotInUse() {
        assertThrows(IllegalArgumentException.class, () -> textures.backingOf(1));
        assertThrows(IllegalArgumentException.class, () -> textures.delete(0, disposer));
        assertThrows(IllegalArgumentException.class, () -> textures.markUsed(9, 1));
    }

    @Test
    void failsWhenEveryNameIsInUse() {
        for (int backing = 101; backing <= 104; backing++) {
            textures.allocate(backing);
        }

        assertThrows(IllegalStateException.class, () -> textures.allocate(105));
    }

    @Test
    void discardsLiveAndWaitingObjectsTogether() {
        int waiting = textures.allocate(101);
        textures.markUsed(waiting, 2);
        textures.delete(waiting, disposer);
        textures.allocate(102);

        assertEquals(2, textures.discardAll());
        assertEquals(0, textures.liveCount());
        assertEquals(0, textures.retiringCount());
        assertEquals(1, textures.allocate(103));
    }

    @Test
    void handsOutDistinctNamesToConcurrentCallers() throws Exception {
        int namesPerCaller = 2_000;
        NameTable shared = new NameTable(ObjectKind.TEXTURE, 2 * namesPerCaller);
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<List<Integer>> low = callers.submit(allocating(shared, 1, namesPerCaller));
            Future<List<Integer>> high = callers.submit(allocating(shared, 1 + namesPerCaller, namesPerCaller));

            Set<Integer> names = new HashSet<>(low.get());
            names.addAll(high.get());

            assertEquals(2 * namesPerCaller, names.size());
            assertEquals(2 * namesPerCaller, shared.liveCount());
        } finally {
            callers.shutdownNow();
        }
    }

    private static Callable<List<Integer>> allocating(NameTable table, int firstBacking, int count) {
        return () -> {
            Integer[] names = new Integer[count];
            for (int offset = 0; offset < count; offset++) {
                names[offset] = table.allocate(firstBacking + offset);
            }
            return Arrays.asList(names);
        };
    }
}
