package io.github.luasgraphicss.minemod.adapters.configuration.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.configuration.recovery.LaunchPhase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LaunchJournalTest {
    @TempDir
    Path gameDirectory;

    private Path file;
    private LaunchJournal journal;

    @BeforeEach
    void locateTheJournal() throws IOException {
        file = ConfigLocation.inGameDirectory(gameDirectory).launchJournal();
        journal = new LaunchJournal(file);
    }

    @Test
    void recordsHowFarEachLaunchGot() throws IOException {
        journal.begin();
        journal.reach(LaunchPhase.FIRST_FRAME);
        journal.reach(LaunchPhase.CLEAN_EXIT);
        journal.begin();

        assertEquals(Arrays.asList(LaunchPhase.CLEAN_EXIT, LaunchPhase.STARTED), journal.launches());
        assertEquals(
                gameDirectory.resolve("config").resolve("minemod-vulkan").resolve("launches.log"), file);
    }

    @Test
    void aLaunchNeverMovesBackToAnEarlierPhase() throws IOException {
        journal.begin();
        journal.reach(LaunchPhase.CLEAN_EXIT);
        journal.reach(LaunchPhase.FIRST_FRAME);

        assertEquals(Collections.singletonList(LaunchPhase.CLEAN_EXIT), journal.launches());
    }

    @Test
    void keepsOnlyTheLatestLaunches() throws IOException {
        for (int launch = 0; launch < 11; launch++) {
            journal.begin();
        }

        assertEquals(8, journal.launches().size());
    }

    @Test
    void ignoresLinesThatAreNotAPhase() throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, Arrays.asList("FIRST_FRAME", "garbage", "", "STARTED"), StandardCharsets.UTF_8);

        assertEquals(Arrays.asList(LaunchPhase.FIRST_FRAME, LaunchPhase.STARTED), journal.launches());
    }

    @Test
    void refusesAPhaseWithoutALaunch() {
        assertThrows(IllegalStateException.class, () -> journal.reach(LaunchPhase.FIRST_FRAME));
    }
}
