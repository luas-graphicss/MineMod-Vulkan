package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.configuration.recovery.LaunchPhase;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlan;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.ModRole;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.Expectation;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.PackMod;
import io.github.luasgraphicss.minemod.verification.ecosystem.shaders.FixturePacks;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PackSessionTest {
    private static final Path REPOSITORY_ROOT = Path.of(System.getProperty("minemod.repositoryRoot"));
    private static final PackMod OVERLAY = new PackMod("overlay", "1.0.0", ModRole.INTERFACE_OVERLAY);
    private static final PackMod MACHINES = new PackMod("machines", "1.0.0", ModRole.TECHNICAL_CONTENT);
    private static final PackMod SHADER_LOADER = new PackMod("shader-loader", "1.0.0", ModRole.SHADER_LOADER);
    private static final Launch FIRST_LAUNCH = Launch.first(FixturePacks.CHAINED_PASSES);

    private FramePlan shaderPlan;

    @BeforeEach
    void planTheFixtureShaderPack() throws IOException {
        shaderPlan = new FixturePacks(REPOSITORY_ROOT)
                .plan(FixturePacks.CHAINED_PASSES, FixturePacks.WITHOUT_COMPUTE)
                .plan()
                .orElseThrow();
    }

    @Test
    void anOverlayReplacesAnElementOnlyWhereTheLoaderCancelsElements() {
        PackRun onForge = play(LoaderTarget.Kind.FORGE, OVERLAY);
        PackRun onFabric = play(LoaderTarget.Kind.FABRIC, OVERLAY);

        assertEquals(List.of(), onForge.failures());
        assertEquals(List.of(), onFabric.failures());
        assertEquals(
                Map.of("draws its own hotbar", 6, "draws its tooltips", 6), onForge.journalsByMod().get("overlay"));
        assertEquals(Map.of("draws its tooltips", 6), onFabric.journalsByMod().get("overlay"));
    }

    @Test
    void twoBridgesOfOneModSubmitInTheSamePassOfEveryFrame() {
        PackRun run = play(LoaderTarget.Kind.NEOFORGE, MACHINES);

        assertEquals(List.of(), run.failures());
        assertEquals(
                Map.of(
                        "attaches a bridge", 2,
                        "detaches a bridge", 2,
                        "submits cable meshes of the translucent pass", 6,
                        "submits fluid surfaces of the translucent pass", 6),
                run.journalsByMod().get("machines"));
        assertTrue(run.summary().endsWith("2 bridge(s) with 12 submission(s), no shader pack"), run.summary());
    }

    @Test
    void aBridgeStaysInactiveForAVersionItsContractDoesNotName() {
        PackRun run = play(LoaderTarget.Kind.FABRIC, new PackMod("machines", "1.1.0", ModRole.TECHNICAL_CONTENT));

        assertEquals(
                List.of(
                        "machines: fluid surfaces of the translucent pass stayed inactive: "
                                + "machines 1.1.0 is not a version the contract was verified on",
                        "machines: cable meshes of the translucent pass stayed inactive: "
                                + "machines 1.1.0 is not a version the contract was verified on"),
                run.failures());
    }

    @Test
    void aRendererVersionWithoutABridgeKeepsTheWorldClosed() {
        PackRun run = play(LoaderTarget.Kind.FABRIC, new PackMod("chunk-renderer", "2.0.0", ModRole.RENDERER_REPLACEMENT));

        assertTrue(run.refusedBeforeWorld());
        assertEquals(
                "refused before the world: TERRAIN_GEOMETRY has no bridge for this exact renderer version, "
                        + "claimed by [chunk-renderer 2.0.0]",
                run.summary());
        assertTrue(run.journalsByMod().isEmpty());
    }

    @Test
    void safeModeRunsTheSamePackWithoutItsShaderPack() {
        Modpack pack = new Modpack(1, "shaders", "1.21.1", LoaderTarget.Kind.FABRIC, List.of(SHADER_LOADER), Expectation.RUNS);
        Launch safe = Launch.after(
                Map.of("shaders.pack", FixturePacks.CHAINED_PASSES), List.of(LaunchPhase.STARTED, LaunchPhase.STARTED));

        PackRun chosen = PackSession.run(pack, shaderPlan, FIRST_LAUNCH);
        PackRun recovering = PackSession.run(pack, shaderPlan, safe);

        assertEquals(List.of(), chosen.failures());
        assertEquals(List.of(), recovering.failures());
        assertTrue(chosen.summary().endsWith("with a shader pack"));
        assertTrue(recovering.summary().endsWith("no shader pack"));
        assertFalse(safe.runsAShaderPack());
        assertEquals(
                List.of(
                        "MineMod Vulkan on Minecraft 1.21.1, fabric",
                        "settings: compatibility.mod-opengl=true; shaders.pack=; diagnostics.overlay=true; "
                                + "recovery.failed-launches-before-safe-mode=2",
                        "settings rejected: none",
                        "renderers: SHADER_PIPELINE bridged for shader-loader 1.0.0",
                        "bridges: shader-loader: passes of the shader pack SATISFIED",
                        "recovery: safe mode, the last 2 launch(es) ended before the first frame"),
                recovering.diagnostics());
    }

    private PackRun play(LoaderTarget.Kind loader, PackMod... mods) {
        Modpack pack = new Modpack(1, "pack", "1.21.1", loader, List.of(mods), Expectation.RUNS);
        return PackSession.run(pack, shaderPlan, FIRST_LAUNCH);
    }
}
