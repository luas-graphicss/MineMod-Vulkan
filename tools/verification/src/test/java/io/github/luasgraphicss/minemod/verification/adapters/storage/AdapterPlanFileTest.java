package io.github.luasgraphicss.minemod.verification.adapters.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlanFile;
import io.github.luasgraphicss.minemod.verification.adapters.fixtures.PinnedWindow;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.adapters.plan.RuntimePlans;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdapterPlanFileTest {
    private static final String NAMES_URL =
            "https://maven.fabricmc.net/net/fabricmc/intermediary/1.13/intermediary-1.13-v2.jar";
    private static final String NAMES_SHA1 = "5da71b4e6431dbfc64f62acb11b40e381a365934";

    @TempDir
    Path directory;

    @Test
    void readsBackAPlanWrittenInTheNamesOfTheGame() throws IOException {
        AdapterPlan official = AdapterPlan.written(
                RuntimePlans.written(PinnedWindow.hooks(), RuntimeNamespace.OFFICIAL), RuntimeNamespace.OFFICIAL);

        AdapterPlan read = AdapterPlanFile.read(fileHolding(AdapterPlanFile.render(official)));

        assertEquals(RuntimeNamespace.OFFICIAL, read.namespace());
        assertEquals("", read.namesUrl());
        assertEquals(AdapterPlanFile.render(official), AdapterPlanFile.render(read));
        assertFalse(AdapterPlanFile.render(official).contains("names.source"));
    }

    @Test
    void readsBackAMappedPlanWithTheFileItsNamesCameFrom() throws IOException {
        AdapterPlan read = AdapterPlanFile.read(fileHolding(AdapterPlanFile.render(mappedPlan())));

        assertEquals(RuntimeNamespace.INTERMEDIARY, read.namespace());
        assertEquals(NAMES_URL, read.namesUrl());
        assertEquals(NAMES_SHA1, read.namesSha1());
        assertEquals(
                PinnedWindow.INTERMEDIARY_SWAP,
                read.plan().redirects().getFirst().point().target().toString());
    }

    @Test
    void writesWhatTheAdapterReadsAtLaunch() throws IOException {
        byte[] rendered = AdapterPlanFile.render(mappedPlan()).getBytes(StandardCharsets.UTF_8);

        ReleasePlan atLaunch = ReleasePlanFile.read(new ByteArrayInputStream(rendered));

        assertEquals(PinnedWindow.RELEASE, atLaunch.release());
        assertTrue(atLaunch.isPinnedFor(PinnedWindow.CLIENT_SHA1));
        assertEquals(1, atLaunch.redirects().getFirst().point().expectedMatches());
    }

    @Test
    void refusesNamesItDoesNotKnow() throws IOException {
        String rendered = AdapterPlanFile.render(mappedPlan()).replace("names=intermediary", "names=yarn");

        IOException refusal = assertThrows(IOException.class, () -> AdapterPlanFile.read(fileHolding(rendered)));

        assertTrue(refusal.getMessage().contains("unknown names: yarn"), refusal.getMessage());
    }

    @Test
    void refusesAMappedPlanThatDoesNotTellItsSource() {
        ReleasePlan plan = RuntimePlans.intermediary(PinnedWindow.hooks(), PinnedWindow.intermediaryNames());

        assertThrows(IllegalArgumentException.class, () -> AdapterPlan.written(plan, RuntimeNamespace.INTERMEDIARY));
    }

    private static AdapterPlan mappedPlan() {
        return AdapterPlan.mapped(
                RuntimePlans.intermediary(PinnedWindow.hooks(), PinnedWindow.intermediaryNames()),
                RuntimeNamespace.INTERMEDIARY,
                NAMES_URL,
                NAMES_SHA1);
    }

    private Path fileHolding(String rendered) throws IOException {
        return Files.writeString(directory.resolve("release-plan.properties"), rendered);
    }
}
