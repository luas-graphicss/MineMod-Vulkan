package io.github.luasgraphicss.minemod.compatibility.dispatch.table;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LibraryImageTest {
    @Test
    void anAddressBelongsToTheSegmentThatCoversIt() {
        LibraryImage image = LibraryImage.ofSegments(0x1000, 0x2000, 0x8000, 0x9000);

        assertTrue(image.contains(0x1000));
        assertTrue(image.contains(0x8FFF));
        assertFalse(image.contains(0x2000));
        assertFalse(image.contains(0x5000));
    }

    @Test
    void addressesInTheUpperHalfCompareAsUnsigned() {
        LibraryImage image = LibraryImage.ofSegments(0xFFFF_8000_0000_0000L, 0xFFFF_8000_0010_0000L);

        assertTrue(image.contains(0xFFFF_8000_0000_1000L));
        assertFalse(image.contains(0x0000_7000_0000_1000L));
    }

    @Test
    void segmentsComeInPairsThatEndAfterTheyStart() {
        assertThrows(IllegalArgumentException.class, LibraryImage::ofSegments);
        assertThrows(IllegalArgumentException.class, () -> LibraryImage.ofSegments(0x1000));
        assertThrows(IllegalArgumentException.class, () -> LibraryImage.ofSegments(0x2000, 0x1000));
    }
}
