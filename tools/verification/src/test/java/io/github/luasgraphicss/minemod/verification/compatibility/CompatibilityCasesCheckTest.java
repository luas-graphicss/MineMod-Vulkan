package io.github.luasgraphicss.minemod.verification.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.check.CheckProfile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityCasesCheckTest {
    @Test
    void committedManifestSatisfiesThePolicy() throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("minemod.repositoryRoot"));

        assertEquals(List.of(), new CompatibilityCasesCheck().findViolations(repositoryRoot));
    }

    @Test
    void runsInTheEcosystemProfileAfterTheTargetMatrix() {
        List<String> checks = CheckProfile.named("ecosystem").orElseThrow().checks().stream()
                .map(Check::name)
                .toList();

        assertEquals(List.of("target-matrix", "compatibility-cases"), checks);
    }
}
