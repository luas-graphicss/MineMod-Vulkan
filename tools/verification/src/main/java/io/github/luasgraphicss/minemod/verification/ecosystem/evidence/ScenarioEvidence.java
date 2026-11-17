package io.github.luasgraphicss.minemod.verification.ecosystem.evidence;

import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ScenarioEvidence {
    private static final String REPOSITORY_DIRECTORY = "docs/compatibility/reports";
    private static final String UNUSED_CACHE = "build/verification-cache";
    private static final long MAX_BYTES = 1024L * 1024;

    private final Path repositoryRoot;

    public ScenarioEvidence(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    public static String repositoryPathOf(RepositoryScenario scenario) {
        return REPOSITORY_DIRECTORY + "/" + scenario.name() + ".txt";
    }

    public ScenarioReport run(RepositoryScenario scenario) throws IOException, InterruptedException {
        return scenario.run(new ScenarioEnvironment(repositoryRoot, repositoryRoot.resolve(UNUSED_CACHE)));
    }

    public void write(RepositoryScenario scenario, ScenarioReport report) throws IOException {
        Path file = repositoryRoot.resolve(repositoryPathOf(scenario));
        Files.createDirectories(file.getParent());
        Files.writeString(file, report.observationsAsText());
    }

    public boolean matches(RepositoryScenario scenario, ScenarioReport report) throws IOException {
        Path file = repositoryRoot.resolve(repositoryPathOf(scenario));
        return Files.isRegularFile(file)
                && Files.size(file) <= MAX_BYTES
                && Files.readString(file).equals(report.observationsAsText());
    }
}
