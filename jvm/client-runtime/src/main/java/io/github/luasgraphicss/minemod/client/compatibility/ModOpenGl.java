package io.github.luasgraphicss.minemod.client.compatibility;

import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.client.compatibility.frame.MixedFrame;
import io.github.luasgraphicss.minemod.client.compatibility.handoff.RecordedFrame;
import io.github.luasgraphicss.minemod.client.compatibility.scene.SharedScene;
import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.DispatchInstaller;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import io.github.luasgraphicss.minemod.compatibility.natives.gate.CallGate;
import io.github.luasgraphicss.minemod.compatibility.provider.diagnosis.ProviderDiagnosis;
import io.github.luasgraphicss.minemod.compatibility.provider.load.ProviderLoadException;
import io.github.luasgraphicss.minemod.compatibility.runtime.CompatibilitySession;
import io.github.luasgraphicss.minemod.compatibility.runtime.ContextPlan;
import io.github.luasgraphicss.minemod.compatibility.runtime.SessionContext;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderRejectedException;
import java.io.IOException;
import java.util.Objects;

public final class ModOpenGl implements AutoCloseable {
    private final CompatibilitySession session;
    private final RecordedFrame recorded;
    private final SharedScene scene;
    private final SessionContext context;
    private final CompatibilityLayer layer;
    private final MixedFrame frame;
    private long presentedFrames;

    private ModOpenGl(
            CompatibilitySession session, ModOpenGlSetup setup, SceneSharing sharing, RecordedFrame recorded) {
        this.session = session;
        this.recorded = Objects.requireNonNull(recorded, "recorded");
        this.scene = new SharedScene(sharing, this::submitPart);
        this.context = session.createContext(
                new ContextPlan(scene, scene::handoffThrough, setup.origins(), setup.wrapperState()));
        this.layer = context.layer();
        this.frame = new MixedFrame(layer);
        CallGate.bind(this::call);
        frame.begin();
    }

    public static ModOpenGl open(ModOpenGlSetup setup, SceneSharing sharing, RecordedFrame recorded) {
        NativeBinding.load(setup.providerNatives());
        CompatibilitySession session = openSession(setup);
        try {
            return new ModOpenGl(session, setup, sharing, recorded);
        } catch (RuntimeException failure) {
            session.close();
            throw failure;
        }
    }

    public ProviderDiagnosis provider() {
        return session.diagnosis();
    }

    public void install(DispatchInstaller installer, BindingGeneration generation) {
        session.install(installer, generation);
    }

    public CompatibilityLayer layer() {
        return layer;
    }

    public SharedScene scene() {
        return scene;
    }

    public void beforeNativeWork() {
        frame.beforeNativeWork();
    }

    public void followSession() {
        scene.followSession(presentedFrames).ifPresent(context::attachScene);
    }

    public void present() {
        frame.present();
        scene.imports().collect(presentedFrames);
        presentedFrames++;
        frame.begin();
    }

    @Override
    public void close() {
        scene.close();
        context.close();
        session.close();
    }

    private boolean submitPart() {
        if (!recorded.submitPart()) {
            return false;
        }
        followSession();
        return true;
    }

    private long call(String function, long[] words) {
        frame.beforeProviderCall();
        return layer.call(function, words);
    }

    private static CompatibilitySession openSession(ModOpenGlSetup setup) {
        ContextRequest context = setup.context();
        try {
            return CompatibilitySession.open(
                    setup.bundle(), context.requirements(), context.majorVersion(), context.minorVersion());
        } catch (IOException | ProviderLoadException | ProviderRejectedException failure) {
            throw new ModOpenGlUnavailable("opening the OpenGL provider in " + setup.bundle().directory(), failure);
        }
    }
}
