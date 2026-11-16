package io.github.luasgraphicss.minemod.compatibility.translation.legacy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DisplayListNamesTest {
    private static final int GL_INVALID_VALUE = 0x0501;
    private static final int GL_OUT_OF_MEMORY = 0x0505;

    private final LayerFixture fixture = new LayerFixture();
    private final DisplayListNames lists =
            new DisplayListNames(() -> fixture.contextNames, fixture.provider, fixture.errors, () -> fixture.frame);

    @Test
    void aGeneratedRangeIsContiguousForTheMod() {
        assertEquals(1, lists.generate(3));

        assertTrue(lists.isList(1));
        assertTrue(lists.isList(3));
        assertFalse(lists.isList(4));
        assertArrayEquals(
                new int[] {RecordingProvider.FIRST_BACKING, RecordingProvider.FIRST_BACKING + 2},
                lists.resolveCalls(new int[] {1, 3}));
    }

    @Test
    void aRangeStartsAfterANameTheModChoseItself() {
        fixture.names(ContextProfile.COMPATIBILITY).backingForUse(ObjectKind.DISPLAY_LIST, 2);

        assertEquals(3, lists.generate(3));
        assertEquals(1, lists.generate(1));
    }

    @Test
    void callListsAddsTheBaseAndSkipsListsThatDoNotExist() {
        int first = lists.generate(4);
        lists.setBase(first);

        assertArrayEquals(
                new int[] {RecordingProvider.FIRST_BACKING + 3, RecordingProvider.FIRST_BACKING, 0},
                lists.resolveCalls(new int[] {3, 0, 40}));
        assertEquals(first, lists.base());
    }

    @Test
    void deletingARangeFreesEveryListInIt() {
        int first = lists.generate(3);

        lists.delete(first, 2);

        assertFalse(lists.isList(first));
        assertTrue(lists.isList(first + 2));
        assertEquals(
                Arrays.asList(
                        "lists " + RecordingProvider.FIRST_BACKING + "+1",
                        "lists " + (RecordingProvider.FIRST_BACKING + 1) + "+1"),
                fixture.provider.disposed());
    }

    @Test
    void aRangeThatDoesNotFitReturnsNoListAndKeepsNone() {
        lists.generate(LayerFixture.NAMES_PER_KIND - 1);

        assertEquals(0, lists.generate(2));

        assertEquals(GL_OUT_OF_MEMORY, fixture.errors.take());
        assertEquals(
                Collections.singletonList("lists " + (RecordingProvider.FIRST_BACKING + LayerFixture.NAMES_PER_KIND - 1) + "+2"),
                fixture.provider.disposed());
        assertTrue(lists.isList(LayerFixture.NAMES_PER_KIND - 1));
        assertFalse(lists.isList(LayerFixture.NAMES_PER_KIND));
    }

    @Test
    void negativeRangesAreInvalidValues() {
        assertEquals(0, lists.generate(-1));
        assertEquals(GL_INVALID_VALUE, fixture.errors.take());

        lists.delete(1, -1);
        assertEquals(GL_INVALID_VALUE, fixture.errors.take());
    }
}
