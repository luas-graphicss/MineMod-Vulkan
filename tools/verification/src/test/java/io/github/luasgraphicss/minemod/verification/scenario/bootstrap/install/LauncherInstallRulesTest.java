package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.hooks.fixtures.CommittedManifests;
import io.github.luasgraphicss.minemod.verification.json.RepositoryJson;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherProfile.Arguments;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherProfile.Artifact;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherProfile.Downloads;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherProfile.Library;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherInstallRulesTest {
    private static final String MODULE = "adapters/bootstrap/mc-1.13";
    private static final String PROFILE_ID = "1.13-minemod-vulkan";
    private static final String AGENT = "io/github/luasgraphicss/minemod-vulkan-bootstrap-1.13/0.1.0/agent.jar";
    private static final String AGENT_ARGUMENT = "-javaagent:${library_directory}/" + AGENT;
    private static final String OWN_CLASS = "io/github/luasgraphicss/minemod/adapters/loaders/agent/Agent.class";
    private static final Library ASM = new Library(
            "org.ow2.asm:asm:9.11.1",
            new Downloads(new Artifact(
                    "org/ow2/asm/asm/9.11.1/asm-9.11.1.jar",
                    "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.11.1/asm-9.11.1.jar",
                    "edb29aa0c2da2918ce338fb7b218c21ea138dde2",
                    131138)));

    private ReleaseTarget release;

    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void findTheReleaseWithoutALoader() throws IOException {
        release = CommittedManifests.targets().releases().stream()
                .filter(target -> target.id().equals("1.13"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void acceptsAProfileThatOnlyAddsTheAgentToTheUnmodifiedGame() throws IOException {
        LauncherInstall install = installOf(profile(release.id(), List.of(), List.of(AGENT_ARGUMENT)), OWN_CLASS);

        assertEquals(List.of(), LauncherInstallRules.findViolations(install, release));
        assertEquals(1, install.agents().size());
        assertTrue(Files.isRegularFile(install.agents().getFirst()));
    }

    @Test
    void findsNoInstallBeforeItIsBuilt() throws IOException {
        assertTrue(LauncherInstall.builtBy(repositoryRoot, MODULE).isEmpty());
    }

    @Test
    void refusesAProfileOfAnotherRelease() throws IOException {
        LauncherInstall install = installOf(profile("1.13.1", List.of(), List.of(AGENT_ARGUMENT)), OWN_CLASS);

        assertEquals(
                List.of("the profile inherits 1.13.1, expected 1.13"),
                LauncherInstallRules.findViolations(install, release));
    }

    @Test
    void refusesAProfileThatChangesHowTheGameIsStarted() throws IOException {
        LauncherInstall install =
                installOf(profile(release.id(), List.of("--demo"), List.of(AGENT_ARGUMENT, "-Xmx8g")), OWN_CLASS);

        List<String> violations = LauncherInstallRules.findViolations(install, release);

        assertEquals(2, violations.size(), violations.toString());
        assertTrue(violations.get(0).contains("--demo"), violations.toString());
        assertTrue(violations.get(1).contains("expected one agent"), violations.toString());
    }

    @Test
    void refusesAnAgentThatCarriesClassesOfTheGame() throws IOException {
        LauncherInstall install =
                installOf(profile(release.id(), List.of(), List.of(AGENT_ARGUMENT)), "net/minecraft/client/main/Main.class");

        List<String> violations = LauncherInstallRules.findViolations(install, release);

        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.getFirst().contains("net/minecraft/client/main/Main.class"), violations.toString());
    }

    @Test
    void refusesAnInstallThatHoldsMoreThanTheProfileAndTheAgent() throws IOException {
        LauncherInstall install = installOf(profile(release.id(), List.of(), List.of(AGENT_ARGUMENT)), OWN_CLASS);
        Files.createFile(install.directory().resolve("versions").resolve(PROFILE_ID).resolve(PROFILE_ID + ".jar"));

        List<String> violations = LauncherInstallRules.findViolations(install, release);

        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.getFirst().startsWith("the install holds more"), violations.toString());
    }

    private LauncherProfile profile(String inherited, List<String> gameArguments, List<String> jvmArguments) {
        return new LauncherProfile(
                PROFILE_ID,
                inherited,
                "release",
                release.releaseTime(),
                release.releaseTime(),
                release.launch().mainClass(),
                new Arguments(gameArguments, jvmArguments),
                List.of(ASM));
    }

    private LauncherInstall installOf(LauncherProfile profile, String agentClass) throws IOException {
        Path install = repositoryRoot.resolve(MODULE).resolve("build/install");
        RepositoryJson.write(install.resolve("versions").resolve(PROFILE_ID).resolve(PROFILE_ID + ".json"), profile);
        Path agent = install.resolve("libraries").resolve(AGENT);
        Files.createDirectories(agent.getParent());
        try (OutputStream file = Files.newOutputStream(agent);
                ZipOutputStream archive = new ZipOutputStream(file)) {
            archive.putNextEntry(new ZipEntry(agentClass));
            archive.closeEntry();
        }
        return LauncherInstall.builtBy(repositoryRoot, MODULE).orElseThrow();
    }
}
