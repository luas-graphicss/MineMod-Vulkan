package io.github.luasgraphicss.minemod.verification.compatibility.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Category;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Evidence;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.ModRelease;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Outcome;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityManifest;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityCaseRulesTest {
    private static final List<ModRelease> MAP_MOD = List.of(new ModRelease("mapmod", "4.2.0+mc1.21.1"));
    private static final String REASON = "the minimap matched the reference capture";
    private static final List<Evidence> CAPTURE =
            List.of(new Evidence("docs/compatibility/evidence/minimap.png", "a".repeat(64)));

    @Test
    void acceptsAVerifiedCaseWithEvidence() throws IOException {
        assertEquals(List.of(), violationsOf(mapCase(Outcome.VERIFIED, REASON, CAPTURE)));
    }

    @Test
    void acceptsACaseThatIsStillUnverified() throws IOException {
        assertEquals(List.of(), violationsOf(mapCase(Outcome.UNVERIFIED, "no laboratory run yet", List.of())));
    }

    @Test
    void rejectsAVerifiedCaseWithoutEvidence() throws IOException {
        assertEquals(
                List.of("case minimap: is verified without evidence"),
                violationsOf(mapCase(Outcome.VERIFIED, REASON, List.of())));
    }

    @Test
    void rejectsAnOutcomeWithoutAReason() throws IOException {
        assertEquals(
                List.of("case minimap: gives no reason for UNSUPPORTED"),
                violationsOf(mapCase(Outcome.UNSUPPORTED, " ", List.of())));
    }

    @Test
    void rejectsEvidenceWithoutASha256Pin() throws IOException {
        List<Evidence> unpinned = List.of(new Evidence("docs/compatibility/evidence/minimap.png", "pending"));

        assertEquals(
                List.of("case minimap: evidence has no path and SHA-256 pin: docs/compatibility/evidence/minimap.png"),
                violationsOf(mapCase(Outcome.VERIFIED, REASON, unpinned)));
    }

    @Test
    void rejectsAReleaseOutsideTheContract() throws IOException {
        assertEquals(
                List.of("case minimap: targets 1.21.4, which is not a pinned release"),
                violationsOf(caseOn("1.21.4", LoaderTarget.Kind.FABRIC, MAP_MOD)));
    }

    @Test
    void rejectsALoaderTheReleaseDoesNotOffer() throws IOException {
        assertEquals(
                List.of("case minimap: targets NEOFORGE on 1.12.2, which the target manifest does not list"),
                violationsOf(caseOn("1.12.2", LoaderTarget.Kind.NEOFORGE, MAP_MOD)));
    }

    @Test
    void rejectsModsOnTheClientBootstrap() throws IOException {
        assertEquals(
                List.of("case minimap: targets the client bootstrap, which loads no mods"),
                violationsOf(caseOn("1.13", LoaderTarget.Kind.CLIENT_BOOTSTRAP, MAP_MOD)));
    }

    @Test
    void rejectsAModVersionThatIsNotExact() throws IOException {
        for (String range : List.of(">=4.2.0", "4.2.*", "[4.2,4.3)", "")) {
            List<ModRelease> mods = List.of(new ModRelease("mapmod", range));

            assertEquals(
                    List.of("case minimap: mod mapmod has no exact version: " + range),
                    violationsOf(caseOn("1.21.1", LoaderTarget.Kind.FABRIC, mods)));
        }
    }

    @Test
    void rejectsACaseWithoutMods() throws IOException {
        assertEquals(
                List.of("case minimap: names no mod release"),
                violationsOf(caseOn("1.21.1", LoaderTarget.Kind.FABRIC, List.of())));
    }

    @Test
    void rejectsACaseListedTwice() throws IOException {
        CompatibilityCase entry = mapCase(Outcome.VERIFIED, REASON, CAPTURE);

        assertEquals(List.of("case minimap is listed twice"), violationsOf(entry, entry));
    }

    @Test
    void rejectsAnotherSchemaVersion() throws IOException {
        CompatibilityManifest manifest = new CompatibilityManifest(2, List.of());

        assertEquals(
                List.of("schemaVersion is 2, expected 1"),
                CompatibilityCaseRules.findViolations(manifest, committedTargets().read()));
    }

    private static CompatibilityCase mapCase(Outcome outcome, String reason, List<Evidence> evidence) {
        return new CompatibilityCase(
                "minimap",
                "1.21.1",
                LoaderTarget.Kind.FABRIC,
                List.of(Category.MAPS),
                MAP_MOD,
                "framebuffer-readback",
                "minimap-texture-update",
                outcome,
                reason,
                evidence);
    }

    private static CompatibilityCase caseOn(String release, LoaderTarget.Kind loader, List<ModRelease> mods) {
        return new CompatibilityCase(
                "minimap",
                release,
                loader,
                List.of(Category.MAPS),
                mods,
                "framebuffer-readback",
                "minimap-texture-update",
                Outcome.VERIFIED,
                REASON,
                CAPTURE);
    }

    private static List<String> violationsOf(CompatibilityCase... cases) throws IOException {
        CompatibilityManifest manifest =
                new CompatibilityManifest(CompatibilityManifest.SCHEMA_VERSION, List.of(cases));
        return CompatibilityCaseRules.findViolations(manifest, committedTargets().read());
    }

    private static TargetManifestFile committedTargets() {
        return TargetManifestFile.inRepository(Path.of(System.getProperty("minemod.repositoryRoot")));
    }
}
