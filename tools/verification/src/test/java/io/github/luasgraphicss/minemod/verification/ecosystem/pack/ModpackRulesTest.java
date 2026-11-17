package io.github.luasgraphicss.minemod.verification.ecosystem.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.Expectation;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.PackMod;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class ModpackRulesTest {
    private static final Path REPOSITORY_ROOT = Path.of(System.getProperty("minemod.repositoryRoot"));
    private static final PackMod OVERLAY = new PackMod("minemod-fixture-overlay", "1.0.0", ModRole.INTERFACE_OVERLAY);

    @Test
    void theCommittedPacksCoverEveryFamilyAndLoaderWithAComposedSuite() throws IOException {
        SortedMap<String, Modpack> packs = ModpackFiles.readFrom(REPOSITORY_ROOT);

        assertEquals(List.of(), ModpackRules.findViolations(packs, targets()));
        assertTrue(packs.containsKey("tests/modpacks/forge-1.12.2-fixture-suite.json"));
    }

    @Test
    void aSuiteMissingForOneLoaderIsAViolation() throws IOException {
        SortedMap<String, Modpack> packs = new TreeMap<>(ModpackFiles.readFrom(REPOSITORY_ROOT));
        packs.remove("tests/modpacks/neoforge-1.21.1-fixture-suite.json");

        List<String> violations = ModpackRules.findViolations(packs, targets());

        assertEquals(1, violations.size());
        assertTrue(violations.get(0).startsWith("no composed suite runs on 1.21 NEOFORGE with "), violations.get(0));
    }

    @Test
    void aPackNamesItsFileAPinnedLoaderAndExactMods() throws IOException {
        Map<String, Modpack> packs = Map.of(
                "tests/modpacks/renamed.json",
                new Modpack(1, "bootstrap-pack", "1.13.1", LoaderTarget.Kind.CLIENT_BOOTSTRAP, List.of(), Expectation.RUNS),
                "tests/modpacks/doubled.json",
                new Modpack(
                        1,
                        "doubled",
                        "1.21.1",
                        LoaderTarget.Kind.FABRIC,
                        List.of(OVERLAY, OVERLAY, new PackMod("minemod-fixture-minimap", "latest release", ModRole.MAP)),
                        Expectation.REFUSED_BEFORE_WORLD));
        TargetManifest onlyNewest = new TargetManifest(
                1, List.of(), targets().releases().stream().filter(release -> release.id().equals("1.21.1")).toList());

        List<String> violations = ModpackRules.findViolations(new TreeMap<>(packs), onlyNewest);

        assertTrue(violations.contains("tests/modpacks/doubled.json: mod minemod-fixture-overlay is installed twice"));
        assertTrue(violations.contains(
                "tests/modpacks/doubled.json: mod minemod-fixture-minimap has no exact version: latest release"));
        assertTrue(violations.contains(
                "tests/modpacks/renamed.json: does not carry schema 1 and the id of its file"));
        assertTrue(violations.contains("tests/modpacks/renamed.json: installs no mod"));
        assertTrue(violations.contains(
                "tests/modpacks/renamed.json: CLIENT_BOOTSTRAP on 1.13.1 is not a pinned mod loader target"));
    }

    private static TargetManifest targets() throws IOException {
        return TargetManifestFile.inRepository(REPOSITORY_ROOT).read();
    }
}
