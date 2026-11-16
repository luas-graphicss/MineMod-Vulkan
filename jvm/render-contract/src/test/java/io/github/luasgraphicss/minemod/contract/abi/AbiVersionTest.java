package io.github.luasgraphicss.minemod.contract.abi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbiVersionTest {
    @Test
    void anUnstableLibraryAcceptsOnlyItsExactVersion() {
        AbiVersion library = new AbiVersion(0, 3);

        assertTrue(library.accepts(new AbiVersion(0, 3)));
        assertFalse(library.accepts(new AbiVersion(0, 2)));
        assertFalse(library.accepts(new AbiVersion(0, 4)));
        assertFalse(library.accepts(new AbiVersion(1, 3)));
    }

    @Test
    void aStableLibraryAcceptsOlderMinorsOfItsMajor() {
        AbiVersion library = new AbiVersion(2, 5);

        assertTrue(library.accepts(new AbiVersion(2, 0)));
        assertTrue(library.accepts(new AbiVersion(2, 5)));
        assertFalse(library.accepts(new AbiVersion(2, 6)));
        assertFalse(library.accepts(new AbiVersion(1, 5)));
    }

    @Test
    void rejectsComponentsThatDoNotFitTheWireFormat() {
        assertThrows(IllegalArgumentException.class, () -> new AbiVersion(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new AbiVersion(0, 0x10000));
    }

    @Test
    void comparesByValue() {
        assertEquals(new AbiVersion(0, 1), AbiVersion.CURRENT);
        assertEquals("0.1", AbiVersion.CURRENT.toString());
    }
}
