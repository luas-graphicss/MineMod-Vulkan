package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OriginLabelTest {
    @Test
    void keepsOnlyTheArchiveNameOfAFileLocation() {
        assertEquals("example-1.0.jar", OriginLabel.of("file:/home/pedro/.minecraft/mods/example-1.0.jar"));
    }

    @Test
    void dropsTheEntryPathOfANestedArchiveLocation() {
        assertEquals("example.jar", OriginLabel.of("jar:file:/C:/Users/Pedro/mods/example.jar!/com/example/Mod.class"));
    }

    @Test
    void readsWindowsSeparators() {
        assertEquals("example.jar", OriginLabel.of("C:\\Users\\Pedro\\mods\\example.jar"));
    }

    @Test
    void namesADirectoryLocationByItsLastFolder() {
        assertEquals("main", OriginLabel.of("file:/home/pedro/project/build/classes/java/main/"));
    }

    @Test
    void reportsALocationWithoutANameAsUnknown() {
        assertEquals(OriginLabel.UNKNOWN, OriginLabel.of(""));
        assertEquals(OriginLabel.UNKNOWN, OriginLabel.of("/"));
    }
}
