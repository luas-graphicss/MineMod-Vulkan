package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import io.github.luasgraphicss.minemod.compatibility.layer.state.TrackedCapability;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.CapabilityRequirements;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.GlVersion;
import io.github.luasgraphicss.minemod.compatibility.provider.load.ProviderLoadException;
import io.github.luasgraphicss.minemod.compatibility.runtime.CompatibilitySession;
import io.github.luasgraphicss.minemod.compatibility.runtime.ContextPlan;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderRejectedException;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.state.reconcile.LeftoverPolicy;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ProviderTestEnvironment {
    public static final String SYSTEM_PROVIDER = "minemod.systemProvider";
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int SCENE_WIDTH = 64;
    public static final int SCENE_HEIGHT = 32;
    private static final String NATIVES = "minemod.providerNatives";
    private static final int MAJOR_VERSION = 4;
    private static final int MINOR_VERSION = 5;
    private static final List<TrackedCapability> WRAPPER_STATE = Arrays.asList(
            new TrackedCapability(GL_BLEND, LeftoverPolicy.RESTORE),
            new TrackedCapability(GL_DEPTH_TEST, LeftoverPolicy.ADOPT));

    private ProviderTestEnvironment() {
    }

    public static ProviderBundle stage(Path target) throws IOException {
        NativeBinding.load(Paths.get(System.getProperty(NATIVES)));
        return StagedProvider.stage(Paths.get(System.getProperty(SYSTEM_PROVIDER)), target);
    }

    public static CompatibilitySession open(ProviderBundle bundle, ContextProfile profile)
            throws IOException, ProviderLoadException, ProviderRejectedException {
        return CompatibilitySession.open(bundle, softwareAllowed(profile), MAJOR_VERSION, MINOR_VERSION);
    }

    public static CapabilityRequirements softwareAllowed(ContextProfile profile) {
        return requirements(profile, true);
    }

    public static CapabilityRequirements hardwareOnly(ContextProfile profile) {
        return requirements(profile, false);
    }

    public static ContextPlan standalonePlan() {
        return planFor(new OwnedSceneTextures(SCENE_WIDTH, SCENE_HEIGHT));
    }

    public static ContextPlan planFor(SceneSource scene) {
        return planFor(scene, ProviderOnlyHandoff::new);
    }

    public static ContextPlan planFor(SceneSource scene, Function<ProviderCalls, AttachmentHandoff> handoff) {
        return new ContextPlan(scene, handoff, className -> Optional.empty(), WRAPPER_STATE);
    }

    private static CapabilityRequirements requirements(ContextProfile profile, boolean softwareDeviceAllowed) {
        return new CapabilityRequirements(
                GlVersion.of(MAJOR_VERSION, MINOR_VERSION), profile, Collections.emptySet(), softwareDeviceAllowed);
    }
}
