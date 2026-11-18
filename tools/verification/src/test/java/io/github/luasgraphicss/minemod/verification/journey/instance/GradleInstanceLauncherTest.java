package io.github.luasgraphicss.minemod.verification.journey.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisabledOnOs(OS.WINDOWS)
class GradleInstanceLauncherTest {
    private static final Duration PATIENCE = Duration.ofSeconds(30);

    @TempDir
    Path repositoryRoot;

    @Test
    void runsTheTaskOfTheInstanceInTheDriverBuildAndKeepsItsOutput() throws IOException, InterruptedException {
        Files.writeString(repositoryRoot.resolve("gradlew"), "echo \"$@\"\nexit 7\n");
        Path mod = repositoryRoot.resolve("mod.jar");

        InstanceExit exit =
                new GradleInstanceLauncher().launch(requestOf(GameInstance.CANDIDATE, Optional.of(mod), PATIENCE));

        assertEquals(7, exit.exitCode());
        assertFalse(exit.timedOut());
        assertEquals(
                "--project-dir " + repositoryRoot.resolve("driver") + " runCandidate -Pjourney.plan="
                        + repositoryRoot.resolve("plan.json") + " -Pjourney.output=" + repositoryRoot.resolve("out")
                        + " --no-daemon --console=plain -Pjourney.mod=" + mod,
                Files.readString(exit.log()).strip());
    }

    @Test
    void theReferenceRunsWithoutTheMod() throws IOException, InterruptedException {
        Files.writeString(repositoryRoot.resolve("gradlew"), "echo \"$@\"\n");

        InstanceExit exit =
                new GradleInstanceLauncher().launch(requestOf(GameInstance.REFERENCE, Optional.empty(), PATIENCE));

        String command = Files.readString(exit.log());
        assertEquals(0, exit.exitCode());
        assertTrue(command.contains(" runReference "), command);
        assertFalse(command.contains("journey.mod"), command);
    }

    @Test
    void stopsAnInstanceThatOutlivesItsTimeout() throws IOException, InterruptedException {
        Files.writeString(repositoryRoot.resolve("gradlew"), "echo started\nsleep 600\n");

        InstanceExit exit = new GradleInstanceLauncher()
                .launch(requestOf(GameInstance.REFERENCE, Optional.empty(), Duration.ofSeconds(2)));

        assertTrue(exit.timedOut());
        assertEquals("started", Files.readString(exit.log()).strip());
    }

    private InstanceRequest requestOf(GameInstance instance, Optional<Path> mod, Duration timeout)
            throws IOException {
        Path output = Files.createDirectories(repositoryRoot.resolve("out"));
        return new InstanceRequest(
                instance,
                repositoryRoot,
                repositoryRoot.resolve("driver"),
                repositoryRoot.resolve("plan.json"),
                output,
                mod,
                timeout);
    }
}
