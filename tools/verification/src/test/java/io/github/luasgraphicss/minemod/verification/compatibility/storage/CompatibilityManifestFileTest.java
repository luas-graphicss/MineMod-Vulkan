package io.github.luasgraphicss.minemod.verification.compatibility.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Category;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Outcome;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompatibilityManifestFileTest {
    private static final String BRIDGED_CASE = """
            {
              "schemaVersion": 1,
              "cases": [
                {
                  "id": "chunk-renderer-terrain",
                  "release": "1.20.1",
                  "loader": "forge",
                  "categories": ["renderer-replacements", "technical-content"],
                  "mods": [{"id": "chunk-renderer", "version": "2.1.0"}],
                  "capability": "terrain-geometry",
                  "scenario": "renderer-conflicts",
                  "outcome": "requires-bridge",
                  "reason": "the renderer replaces terrain drawing",
                  "evidence": []
                }
              ]
            }
            """;

    @TempDir
    Path repositoryRoot;

    @Test
    void readsACaseWithItsTargetModsAndOutcome() throws IOException {
        writeManifest(BRIDGED_CASE);

        CompatibilityCase entry = CompatibilityManifestFile.readFrom(repositoryRoot).cases().get(0);

        assertEquals("1.20.1", entry.release());
        assertEquals(LoaderTarget.Kind.FORGE, entry.loader());
        assertEquals(List.of(Category.RENDERER_REPLACEMENTS, Category.TECHNICAL_CONTENT), entry.categories());
        assertEquals("2.1.0", entry.mods().get(0).version());
        assertEquals(Outcome.REQUIRES_BRIDGE, entry.outcome());
    }

    @Test
    void rejectsAnOutcomeOutsideTheFourStates() throws IOException {
        writeManifest(BRIDGED_CASE.replace("requires-bridge", "works"));

        assertThrows(IOException.class, () -> CompatibilityManifestFile.readFrom(repositoryRoot));
    }

    @Test
    void rejectsACaseThatOmitsItsReason() throws IOException {
        writeManifest(BRIDGED_CASE.replace("\"reason\": \"the renderer replaces terrain drawing\",", ""));

        assertThrows(IOException.class, () -> CompatibilityManifestFile.readFrom(repositoryRoot));
    }

    @Test
    void rejectsANullInPlaceOfTheEvidence() throws IOException {
        writeManifest(BRIDGED_CASE.replace("\"evidence\": []", "\"evidence\": null"));

        assertThrows(IOException.class, () -> CompatibilityManifestFile.readFrom(repositoryRoot));
    }

    private void writeManifest(String content) throws IOException {
        Path manifest = repositoryRoot.resolve("manifests/compatibility-cases.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, content);
    }
}
