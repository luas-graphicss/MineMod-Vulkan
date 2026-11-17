package io.github.luasgraphicss.minemod.verification.journey.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.journey.fixtures.JourneyRepository;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlanFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceDirectoryTest {
    private static final Path PACKS = JourneyRepository.committed().resolve("tests/integration/vanilla-journey/packs");

    @TempDir
    Path directory;

    @Test
    void writesTheOptionsAndInstallsThePacksOfThePlan() throws IOException {
        JourneyPlan plan = JourneyPlanFile.inRepository(JourneyRepository.committed(), "resource-reload").read();

        InstanceDirectory.prepare(directory, plan, PACKS);

        List<String> options = Files.readAllLines(directory.resolve("game/options.txt"));
        assertEquals(plan.options().size(), options.size());
        assertEquals(options.stream().sorted().toList(), options);
        assertTrue(options.contains("overrideWidth:854"), options.toString());
        assertTrue(options.contains("renderClouds:\"false\""), options.toString());
        for (String pack : plan.packs()) {
            Path installed = directory.resolve("game/resourcepacks").resolve(pack);
            assertTrue(Files.isRegularFile(installed.resolve("pack.mcmeta")), pack);
            assertTrue(
                    Files.isRegularFile(installed.resolve("assets/minecraft/shaders/core/position_tex_color.fsh")),
                    pack);
        }
    }

    @Test
    void startsFromAnEmptyDirectory() throws IOException {
        JourneyPlan plan = JourneyPlanFile.inRepository(JourneyRepository.committed(), "vanilla-journey").read();
        Path stale = InstanceDirectory.captureIn(directory, "overworld");
        Files.createDirectories(stale.getParent());
        Files.createFile(stale);

        InstanceDirectory.prepare(directory, plan, PACKS);

        assertFalse(Files.exists(stale));
        assertEquals(directory.resolve("captures/overworld.png"), stale);
    }
}
