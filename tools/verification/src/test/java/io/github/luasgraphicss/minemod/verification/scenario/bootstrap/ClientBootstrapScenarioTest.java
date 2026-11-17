package io.github.luasgraphicss.minemod.verification.scenario.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientBootstrapScenarioTest {
    private static final List<String> MANIFESTS =
            List.of("manifests/minecraft-targets.json", "manifests/presentation-hooks.json");

    @TempDir
    Path repositoryRoot;

    @Test
    void asksForTheInstallsBeforeItDownloadsAnyClient() throws IOException, InterruptedException {
        Path committed = Path.of(System.getProperty("minemod.repositoryRoot"));
        for (String manifest : MANIFESTS) {
            Files.createDirectories(repositoryRoot.resolve(manifest).getParent());
            Files.copy(committed.resolve(manifest), repositoryRoot.resolve(manifest));
        }
        Path cache = repositoryRoot.resolve("cache");

        ScenarioReport report =
                new ClientBootstrapScenario().run(new ScenarioEnvironment(repositoryRoot, cache));

        assertEquals(
                List.of(
                        "1.13: adapters/bootstrap/mc-1.13 has no launcher install; build it with"
                                + " \"gradlew :adapters:bootstrap:mc-1.13:launcherInstall\"",
                        "1.13.1: adapters/bootstrap/mc-1.13.1 has no launcher install; build it with"
                                + " \"gradlew :adapters:bootstrap:mc-1.13.1:launcherInstall\""),
                report.failures());
        assertEquals(List.of(), report.observations());
        assertEquals(false, Files.exists(cache));
    }
}
