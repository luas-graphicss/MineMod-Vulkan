package io.github.luasgraphicss.minemod.verification.scenario.lifecycle;

import io.github.luasgraphicss.minemod.compatibility.contexts.CompatibilityContext;
import io.github.luasgraphicss.minemod.compatibility.contexts.ContextRegistry;
import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.handle.HandleLedger;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingNegotiator;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingOutcome;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import io.github.luasgraphicss.minemod.compatibility.interop.resource.SharedResource;
import io.github.luasgraphicss.minemod.compatibility.interop.resource.SharedResourcePool;
import io.github.luasgraphicss.minemod.compatibility.resources.alias.VanillaAliases;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ObjectDisposer;
import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedContexts;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedResources;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SharedResourceLifecycleScenario implements Scenario {
    private static final int CYCLES = 1_000;
    private static final int FAILED_IMPORT_PERIOD = 13;
    private static final int NAMES_PER_KIND = 32;
    private static final int RGBA8 = 37;
    private static final int D24S8 = 129;
    private static final long BLOCK_ATLAS = 1;
    private static final List<SharingRequest> REQUESTS = List.of(
            SharingRequest.image(ShareableResource.COLOR_ATTACHMENT, RGBA8, 1920, 1080, 1, 1),
            SharingRequest.image(ShareableResource.DEPTH_STENCIL_ATTACHMENT, D24S8, 1920, 1080, 1, 1),
            SharingRequest.image(ShareableResource.TEXTURE, RGBA8, 1024, 1024, 11, 1),
            SharingRequest.buffer(1 << 20));
    private static final ObjectDisposer NO_PROVIDER_OBJECT = (kind, backing) -> { };

    @Override
    public String name() {
        return "shared-resource-lifecycle";
    }

    @Override
    public ScenarioReport run() {
        ScenarioLog log = new ScenarioLog();
        for (HandlePlatform platform : HandlePlatform.values()) {
            cycleSharedResources(platform, log);
        }
        checkRejections(log);
        cycleContextsAndAliases(log);
        return log.report();
    }

    private static void cycleSharedResources(HandlePlatform platform, ScenarioLog log) {
        SimulatedResources resources = new SimulatedResources(platform);
        HandleLedger handles = new HandleLedger(platform, resources);
        SharedResourcePool pool = new SharedResourcePool(resources, handles);
        DeviceIdentity gpu = identity(1);
        SharingOutcome outcome = SharingNegotiator.negotiate(
                gpu, gpu, platform.requiredExtensions(), platform, REQUESTS, resources);
        if (!outcome.isAccepted()) {
            log.fail(platform + " sharing plan was rejected: " + outcome.rejections());
            return;
        }

        int refusedImports = 0;
        for (int cycle = 0; cycle < CYCLES; cycle++) {
            List<SharedResource> generation = createAll(pool, outcome.shares());
            if (cycle % FAILED_IMPORT_PERIOD == 0) {
                refusedImports += refusedImport(pool, resources, outcome.shares().get(0)) ? 1 : 0;
            }
            List<SharedResource> reloaded = createAll(pool, outcome.shares());
            for (SharedResource resource : generation) {
                pool.retire(resource, cycle);
            }
            log.expectEqual(0, pool.collect(cycle - 1), platform + " resources freed while frame " + cycle + " ran");
            for (SharedResource resource : reloaded) {
                pool.retire(resource, cycle);
            }
            pool.collect(cycle);
        }

        log.expectEqual(0, pool.liveCount() + pool.retiringCount(), platform + " resources left in the pool");
        log.expectEqual(0, handles.ownedCount(), platform + " handles still owned");
        log.expectEqual(0, resources.leftoverCount(), platform + " objects, resources or handles left open");
        log.observe(platform + ": " + CYCLES + " create, reload and destroy cycles of " + REQUESTS.size()
                + " shared resources, " + refusedImports + " refused imports, nothing left open");
    }

    private static List<SharedResource> createAll(SharedResourcePool pool, List<PlannedShare> shares) {
        List<SharedResource> created = new ArrayList<>(shares.size());
        for (PlannedShare share : shares) {
            created.add(pool.create(share));
        }
        return created;
    }

    private static boolean refusedImport(SharedResourcePool pool, SimulatedResources resources, PlannedShare share) {
        resources.failImports(true);
        try {
            pool.create(share);
            return false;
        } catch (IllegalStateException refused) {
            return true;
        } finally {
            resources.failImports(false);
        }
    }

    private static void checkRejections(ScenarioLog log) {
        HandlePlatform platform = HandlePlatform.LINUX_FILE_DESCRIPTOR;
        SimulatedResources resources = new SimulatedResources(platform);
        SharingOutcome otherGpu = SharingNegotiator.negotiate(
                identity(1), identity(2), platform.requiredExtensions(), platform, REQUESTS, resources);
        SharingOutcome withoutSemaphores = SharingNegotiator.negotiate(
                identity(1), identity(1), List.of("GL_EXT_memory_object", "GL_EXT_memory_object_fd"), platform,
                REQUESTS, resources);

        log.expect(!otherGpu.isAccepted() && otherGpu.shares().isEmpty(), "a different GPU to be rejected");
        log.expectEqual(2, withoutSemaphores.rejections().size(), "missing semaphore extensions named");
        log.expectEqual(0, resources.leftoverCount(), "resources created for a rejected plan");
        log.observe("a different GPU and a provider without semaphore extensions were rejected before sharing");
    }

    private static void cycleContextsAndAliases(ScenarioLog log) {
        SimulatedContexts backend = new SimulatedContexts();
        ContextRegistry contexts = new ContextRegistry(backend, NAMES_PER_KIND);
        for (int cycle = 0; cycle < CYCLES; cycle++) {
            CompatibilityContext first = contexts.create();
            CompatibilityContext second = contexts.createSharingWith(first);
            contexts.makeCurrent(first);
            NameTable textures = first.names().table(ObjectKind.TEXTURE);
            VanillaAliases aliases = new VanillaAliases(textures);
            int atlas = aliases.publish(BLOCK_ATLAS, 1, NO_PROVIDER_OBJECT);
            textures.markUsed(atlas, cycle);
            aliases.publish(BLOCK_ATLAS, 2, NO_PROVIDER_OBJECT);
            first.names().table(ObjectKind.FRAMEBUFFER).allocate(3);

            contexts.destroy(first);
            contexts.makeCurrent(second);
            log.expect(
                    second.names().table(ObjectKind.TEXTURE).contains(atlas),
                    "the shared atlas to outlive the context that published it");
            contexts.destroy(second);
        }

        log.expectEqual(0, contexts.liveContextCount(), "contexts left");
        log.expectEqual(0, contexts.liveShareGroupCount(), "share groups left");
        log.expectEqual(0, contexts.liveObjectCount(), "object names left");
        log.expectEqual(0, backend.liveCount(), "provider contexts left");
        log.observe(CYCLES + " cycles of two sharing contexts destroyed out of order, no context or name left");
    }

    private static DeviceIdentity identity(int seed) {
        byte[] deviceUuid = new byte[DeviceIdentity.UUID_BYTES];
        byte[] driverUuid = new byte[DeviceIdentity.UUID_BYTES];
        Arrays.fill(deviceUuid, (byte) seed);
        Arrays.fill(driverUuid, (byte) (seed + 1));
        return new DeviceIdentity(deviceUuid, driverUuid);
    }
}
