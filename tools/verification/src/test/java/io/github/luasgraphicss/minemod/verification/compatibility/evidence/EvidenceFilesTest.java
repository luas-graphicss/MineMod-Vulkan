package io.github.luasgraphicss.minemod.verification.compatibility.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Category;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Evidence;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.ModRelease;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Outcome;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityManifest;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvidenceFilesTest {
    private static final String CAPTURE = "docs/compatibility/evidence/minimap.txt";
    private static final String MISSING = "docs/compatibility/evidence/missing.txt";
    private static final String CAPTURE_SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @TempDir
    Path repositoryRoot;

    @Test
    void acceptsEvidenceThatMatchesItsPin() throws IOException {
        writeCapture("abc");

        assertEquals(List.of(), violationsOf(new Evidence(CAPTURE, CAPTURE_SHA256)));
    }

    @Test
    void rejectsEvidenceThatChangedAfterBeingPinned() throws IOException {
        writeCapture("abd");

        assertEquals(
                List.of("case minimap: evidence " + CAPTURE + " does not match its SHA-256 pin"),
                violationsOf(new Evidence(CAPTURE, CAPTURE_SHA256)));
    }

    @Test
    void rejectsEvidenceThatDoesNotExist() throws IOException {
        assertEquals(
                List.of("case minimap: evidence " + CAPTURE + " does not exist"),
                violationsOf(new Evidence(CAPTURE, CAPTURE_SHA256)));
    }

    @Test
    void rejectsEvidenceOutsideTheRepository() throws IOException {
        assertEquals(
                List.of("case minimap: evidence ../minimap.txt is outside the repository"),
                violationsOf(new Evidence("../minimap.txt", CAPTURE_SHA256)));
    }

    @Test
    void pinsACaseToTheEvidenceOnDiskAndKeepsThePinOfAMissingFile() throws IOException {
        writeCapture("abc");
        CompatibilityManifest stale = manifestOf(new Evidence(CAPTURE, "0".repeat(64)), new Evidence(MISSING, "f".repeat(64)));

        CompatibilityManifest pinned = EvidenceFiles.pinnedToFiles(repositoryRoot, stale);

        assertEquals(
                List.of(new Evidence(CAPTURE, CAPTURE_SHA256), new Evidence(MISSING, "f".repeat(64))),
                pinned.cases().get(0).evidence());
    }

    private void writeCapture(String content) throws IOException {
        Path capture = repositoryRoot.resolve(CAPTURE);
        Files.createDirectories(capture.getParent());
        Files.write(capture, content.getBytes(StandardCharsets.UTF_8));
    }

    private List<String> violationsOf(Evidence evidence) throws IOException {
        return EvidenceFiles.findViolations(repositoryRoot, manifestOf(evidence));
    }

    private static CompatibilityManifest manifestOf(Evidence... evidence) {
        CompatibilityCase entry = new CompatibilityCase(
                "minimap",
                "1.21.1",
                LoaderTarget.Kind.FABRIC,
                List.of(Category.MAPS),
                List.of(new ModRelease("mapmod", "4.2.0")),
                "framebuffer-readback",
                "minimap-texture-update",
                Outcome.VERIFIED,
                "the minimap matched the reference capture",
                List.of(evidence));
        return new CompatibilityManifest(CompatibilityManifest.SCHEMA_VERSION, List.of(entry));
    }
}
