package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import io.github.luasgraphicss.minemod.adapters.events.dispatch.ElementPhase;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.adapters.events.resources.ResourceExtensionPoint;
import io.github.luasgraphicss.minemod.adapters.events.resources.ResourceGenerations;
import io.github.luasgraphicss.minemod.adapters.ownership.gate.WorldLoadGate;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlan;
import io.github.luasgraphicss.minemod.compatibility.bridges.activation.BridgeActivation;
import io.github.luasgraphicss.minemod.compatibility.bridges.activation.BridgeCandidate;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractVerdict;
import io.github.luasgraphicss.minemod.compatibility.bridges.registry.BridgeRegistry;
import io.github.luasgraphicss.minemod.compatibility.bridges.session.BridgeHost;
import io.github.luasgraphicss.minemod.compatibility.bridges.session.BridgeSession;
import io.github.luasgraphicss.minemod.compatibility.spi.SceneAttachments;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Dispatch;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Priority;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureLoaders;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureRenderers;
import io.github.luasgraphicss.minemod.verification.ecosystem.mods.CaptureMod;
import io.github.luasgraphicss.minemod.verification.ecosystem.mods.FixtureBridge;
import io.github.luasgraphicss.minemod.verification.ecosystem.mods.MapMod;
import io.github.luasgraphicss.minemod.verification.ecosystem.mods.ModelsMod;
import io.github.luasgraphicss.minemod.verification.ecosystem.mods.OptimizationMod;
import io.github.luasgraphicss.minemod.verification.ecosystem.mods.OverlayMod;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.ModRole;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.PackMod;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PackSession {
    private static final int WORLDS = 2;
    private static final int FRAMES_PER_WORLD = 3;
    private static final SceneAttachments ATTACHMENTS = new SceneAttachments(854, 480, 1);

    private final Modpack pack;
    private final FramePlan shaderPlan;
    private final Launch launch;
    private final Dispatch dispatch;
    private final SessionServices services;
    private final PackMods installed;
    private final Map<String, ModContext> contexts = new LinkedHashMap<>();
    private final List<BridgeCandidate> candidates = new ArrayList<>();
    private final BridgeRegistry registry = new BridgeRegistry();
    private int submissions;
    private int hotbarEventsSeenAtLowestPriority;
    private int canceledHotbarEventsReceived;

    private PackSession(Modpack pack, FramePlan shaderPlan, Launch launch) {
        this.pack = pack;
        this.shaderPlan = shaderPlan;
        this.launch = launch;
        this.dispatch = FixtureLoaders.dispatchOf(pack.loader());
        Set<ResourceExtensionPoint> extended = pack.roles().contains(ModRole.CUSTOM_MODELS)
                ? EnumSet.of(ResourceExtensionPoint.MODEL_BAKING, ResourceExtensionPoint.ATLAS_STITCHING)
                : EnumSet.noneOf(ResourceExtensionPoint.class);
        this.services = new SessionServices(new FixtureBus(dispatch), new ResourceGenerations(extended));
        this.installed = new PackMods(pack.installedVersions());
    }

    public static PackRun run(Modpack pack, FramePlan shaderPlan, Launch launch) {
        return new PackSession(pack, shaderPlan, launch).play();
    }

    private PackRun play() {
        FixtureRenderers renderers = new FixtureRenderers();
        pack.mods().forEach(mod -> install(mod, renderers));
        WorldLoadGate gate = renderers.gateFor(pack.installedVersions());
        if (!gate.diagnosis().isEmpty()) {
            List<String> diagnostics = SessionDiagnostics.describe(pack, launch, gate.decision(), Map.of());
            return new PackRun(
                    true, List.of(), Map.of(), diagnostics, "refused before the world: " + String.join("; ", gate.diagnosis()));
        }
        Map<String, ContractVerdict> verdicts = new BridgeActivation(registry).activate(candidates, installed);
        verdicts.forEach((bridge, verdict) -> {
            if (!verdict.activates()) {
                services.failures.add(bridge + " stayed inactive: " + verdict.explanation());
            }
        });
        BridgeSession bridges = new BridgeSession(registry, host());
        bridges.attachAll();
        watchTheReplacedElement();
        reloadResources();
        Optional<ShaderFrames> shaders = pack.roles().contains(ModRole.SHADER_LOADER) && launch.runsAShaderPack()
                ? Optional.of(new ShaderFrames(shaderPlan))
                : Optional.empty();
        FrameScript script = new FrameScript(FixtureLoaders.contractOf(pack.loader()), services, bridges, shaders);
        for (int world = 0; world < WORLDS; world++) {
            services.clock.enterWorld();
            for (int frame = 0; frame < FRAMES_PER_WORLD; frame++) {
                checkNativeDraws(script.render());
            }
            services.worldUnloadHooks.forEach(Runnable::run);
            if (!services.ledger.live().isEmpty()) {
                services.failures.add("world " + services.clock.world() + " left " + services.ledger.live());
            }
        }
        shaders.ifPresent(pipeline -> {
            if (!pipeline.close()) {
                services.failures.add("the shader pipeline kept targets after it closed");
            }
        });
        services.failures.addAll(bridges.detachAll());
        checkCancellationOrder();
        List<String> diagnostics = SessionDiagnostics.describe(pack, launch, gate.decision(), verdicts);
        return new PackRun(false, services.failures, journals(), diagnostics, summary(verdicts.size(), shaders.isPresent()));
    }

    private void install(PackMod mod, FixtureRenderers renderers) {
        ModContext context = new ModContext(mod.id(), services);
        contexts.put(mod.id(), context);
        switch (mod.role()) {
            case GAMEPLAY -> context.note("draws nothing");
            case INTERFACE_OVERLAY -> OverlayMod.install(context);
            case MAP -> MapMod.install(context);
            case CUSTOM_MODELS -> ModelsMod.install(context);
            case CPU_OPTIMIZATION -> OptimizationMod.install(context);
            case CAPTURE -> CaptureMod.install(context);
            case TECHNICAL_CONTENT -> {
                bridge(FixtureBridge.FLUID_SURFACES, context);
                bridge(FixtureBridge.CABLE_MESHES, context);
            }
            case RENDERER_REPLACEMENT -> {
                renderers.add(mod.id(), PipelineStage.TERRAIN_GEOMETRY);
                bridge(FixtureBridge.TERRAIN_MESHES, context);
            }
            case SHADER_LOADER -> {
                renderers.add(mod.id(), PipelineStage.SHADER_PIPELINE);
                bridge(FixtureBridge.SHADER_PASSES, context);
            }
        }
    }

    private void bridge(String operation, ModContext context) {
        candidates.add(FixtureBridge.candidate(operation, context));
        installed.declare(context.modId(), FixtureBridge.symbolOf(operation));
    }

    private BridgeHost host() {
        return new BridgeHost() {
            @Override
            public SceneAttachments attachments() {
                return ATTACHMENTS;
            }

            @Override
            public void submit(String bridgeId, ByteBuffer commandStream) {
                if (!commandStream.isReadOnly() || commandStream.remaining() == 0) {
                    services.failures.add(bridgeId + " submitted a stream the host cannot trust");
                }
                submissions++;
            }
        };
    }

    private void watchTheReplacedElement() {
        services.bus.onElement(
                FramePoint.HUD_ELEMENTS, OverlayMod.REPLACED_ELEMENT, ElementPhase.BEFORE, Priority.LOWEST, false, canceled -> {
                    hotbarEventsSeenAtLowestPriority++;
                    return false;
                });
        services.bus.onElement(
                FramePoint.HUD_ELEMENTS, OverlayMod.REPLACED_ELEMENT, ElementPhase.BEFORE, Priority.LOWEST, true, canceled -> {
                    if (canceled) {
                        canceledHotbarEventsReceived++;
                    }
                    return false;
                });
    }

    private void reloadResources() {
        services.generations.beginReload();
        services.reloadHooks.forEach(Runnable::run);
        services.clock.publish(services.generations.publish());
    }

    private void checkNativeDraws(List<String> nativeDraws) {
        List<String> expected = new ArrayList<>(List.of(FrameScript.BLOCK_OUTLINE, FrameScript.HAND));
        expected.addAll(FrameScript.HUD_ELEMENTS);
        if (replacesAnElement()) {
            expected.remove(OverlayMod.REPLACED_ELEMENT);
        }
        if (!nativeDraws.equals(expected)) {
            services.failures.add(
                    "frame " + services.clock.frame() + " drew " + nativeDraws + " natively, expected " + expected);
        }
    }

    private void checkCancellationOrder() {
        int frames = WORLDS * FRAMES_PER_WORLD;
        int elementEvents = dispatch == Dispatch.PRIORITY_BUS ? frames : 0;
        int expectedAtLowest = replacesAnElement() ? 0 : elementEvents;
        int expectedCanceled = replacesAnElement() ? elementEvents : 0;
        if (hotbarEventsSeenAtLowestPriority != expectedAtLowest || canceledHotbarEventsReceived != expectedCanceled) {
            services.failures.add("a canceled element reached " + hotbarEventsSeenAtLowestPriority
                    + " plain listener(s) and " + canceledHotbarEventsReceived + " listener(s) that receive canceled events,"
                    + " expected " + expectedAtLowest + " and " + expectedCanceled);
        }
    }

    private boolean replacesAnElement() {
        return dispatch == Dispatch.PRIORITY_BUS && pack.roles().contains(ModRole.INTERFACE_OVERLAY);
    }

    private Map<String, Map<String, Integer>> journals() {
        Map<String, Map<String, Integer>> journals = new LinkedHashMap<>();
        contexts.forEach((modId, context) -> journals.put(modId, context.journal()));
        return journals;
    }

    private String summary(int bridges, boolean shaderPack) {
        return pack.mods().size() + " mod(s) on a " + dispatch + ", " + WORLDS * FRAMES_PER_WORLD + " frames in " + WORLDS
                + " worlds, " + bridges + " bridge(s) with " + submissions + " submission(s), "
                + (shaderPack ? "with a shader pack" : "no shader pack");
    }
}
