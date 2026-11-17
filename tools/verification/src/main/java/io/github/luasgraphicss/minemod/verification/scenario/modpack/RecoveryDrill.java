package io.github.luasgraphicss.minemod.verification.scenario.modpack;

import io.github.luasgraphicss.minemod.adapters.configuration.recovery.LaunchPhase;
import io.github.luasgraphicss.minemod.adapters.configuration.settings.RendererSettings;
import io.github.luasgraphicss.minemod.adapters.configuration.storage.ConfigLocation;
import io.github.luasgraphicss.minemod.adapters.configuration.storage.LaunchJournal;
import io.github.luasgraphicss.minemod.adapters.configuration.storage.SettingsFile;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlan;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.Launch;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.PackRun;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.PackSession;
import io.github.luasgraphicss.minemod.verification.ecosystem.shaders.FixturePacks;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class RecoveryDrill {
    private static final byte[] WORLD_BYTES = {0x0A, 0x00, 0x00, 0x0A, 0x00, 0x04, 0x44, 0x61, 0x74, 0x61};
    private static final Map<String, String> CHOSEN_SETTINGS =
            Map.of(RendererSettings.SHADER_PACK.key(), FixturePacks.CHAINED_PASSES);

    private RecoveryDrill() {}

    static void run(Modpack pack, FramePlan shaderPlan, ScenarioLog log) throws IOException {
        Path gameDirectory = Files.createTempDirectory("minemod-recovery-drill");
        try {
            Path world = Files.createDirectories(gameDirectory.resolve("saves").resolve("New World")).resolve("level.dat");
            Files.write(world, WORLD_BYTES);
            ConfigLocation location = ConfigLocation.inGameDirectory(gameDirectory);
            SettingsFile settings = new SettingsFile(location.settingsFile());
            LaunchJournal journal = new LaunchJournal(location.launchJournal());
            settings.write(CHOSEN_SETTINGS);
            journal.begin();
            journal.begin();

            PackRun recovering = launch(pack, shaderPlan, settings, journal);
            journal.reach(LaunchPhase.FIRST_FRAME);
            PackRun recovered = launch(pack, shaderPlan, settings, journal);

            log.expect(
                    recovering.summary().contains("no shader pack") && recovering.failures().isEmpty(),
                    "the launch after two that showed no frame to run in safe mode without the shader pack");
            log.expect(
                    recovered.summary().contains("with a shader pack") && recovered.failures().isEmpty(),
                    "the launch after a frame was shown to run the chosen shader pack again");
            log.expectEqual(CHOSEN_SETTINGS, settings.read(), "settings file after safe mode");
            log.expectEqual(List.of(world), filesUnder(gameDirectory.resolve("saves")), "files among the worlds");
            log.expect(
                    Arrays.equals(WORLD_BYTES, Files.readAllBytes(world)), "the world file to keep its bytes");
            recovering.diagnostics().forEach(line -> log.observe("recovery drill on " + pack.id() + ": " + line));
        } finally {
            deleteTree(gameDirectory);
        }
    }

    private static PackRun launch(Modpack pack, FramePlan shaderPlan, SettingsFile settings, LaunchJournal journal)
            throws IOException {
        Launch launch = Launch.after(settings.read(), journal.launches());
        journal.begin();
        return PackSession.run(pack, shaderPlan, launch);
    }

    private static List<Path> filesUnder(Path directory) throws IOException {
        try (Stream<Path> tree = Files.walk(directory)) {
            return tree.filter(Files::isRegularFile).toList();
        }
    }

    private static void deleteTree(Path directory) throws IOException {
        try (Stream<Path> tree = Files.walk(directory)) {
            for (Path path : tree.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
