package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.compatibility;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.frame.GameFrame;
import io.github.luasgraphicss.minemod.client.compatibility.ContextRequest;
import io.github.luasgraphicss.minemod.client.compatibility.ModOpenGl;
import io.github.luasgraphicss.minemod.client.compatibility.ModOpenGlSetup;
import io.github.luasgraphicss.minemod.client.compatibility.state.WrapperCall;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.OriginResolver;
import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.DispatchInstaller;
import io.github.luasgraphicss.minemod.compatibility.lwjgl3.Lwjgl3Dispatch;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import java.nio.file.Path;
import java.util.Optional;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public final class ModRendering {
    private static final int CONTEXT_MAJOR = 4;
    private static final int CONTEXT_MINOR = 5;

    private static volatile ModOpenGlSetup prepared;
    private static volatile ModOpenGl mods;
    private static Lwjgl3Dispatch binding;

    private ModRendering() {}

    public static void prepare(
            Path providerNatives, ProviderBundle provider, OriginResolver origins, boolean softwareDeviceAllowed) {
        prepared = new ModOpenGlSetup(
                providerNatives,
                provider,
                new ContextRequest(CONTEXT_MAJOR, CONTEXT_MINOR, ContextProfile.CORE, softwareDeviceAllowed),
                origins,
                WrapperCapabilities.tracked());
    }

    public static boolean isPrepared() {
        return prepared != null;
    }

    public static void attach(ClientRenderer renderer) {
        ModOpenGlSetup setup = prepared;
        if (setup == null) {
            return;
        }
        Lwjgl3Dispatch dispatch = new Lwjgl3Dispatch();
        ModOpenGl opened = ModOpenGl.open(setup, renderer.sharing(), () -> GameFrame.submitPart(renderer));
        try {
            opened.install(new DispatchInstaller(dispatch, dispatch), BindingGeneration.LWJGL3);
            opened.mirror(renderer.openGl().textures());
        } catch (RuntimeException failure) {
            opened.close();
            throw failure;
        }
        binding = dispatch;
        mods = opened;
    }

    public static Optional<ModOpenGl> running() {
        return Optional.ofNullable(mods);
    }

    public static GLCapabilities capabilities() {
        return mods == null ? null : GL.createCapabilities();
    }

    public static void capability(int capability, boolean isEnabled) {
        ModOpenGl open = mods;
        if (open != null) {
            open.layer().wrapperState().set(capability, isEnabled);
        }
    }

    public static void left(WrapperCall call, long argument) {
        ModOpenGl open = mods;
        if (open != null) {
            open.wrapperCalls().keep(call, argument);
        }
    }

    public static void left(WrapperCall call, long first, long second) {
        ModOpenGl open = mods;
        if (open != null) {
            open.wrapperCalls().keep(call, first, second);
        }
    }

    public static void left(WrapperCall call, long first, long second, long third, long fourth) {
        ModOpenGl open = mods;
        if (open != null) {
            open.wrapperCalls().keep(call, first, second, third, fourth);
        }
    }

    public static void beforeNativeWork() {
        ModOpenGl open = mods;
        if (open != null) {
            open.beforeNativeWork();
        }
    }

    public static void present() {
        ModOpenGl open = mods;
        if (open != null) {
            open.present();
        }
    }

    public static void detach() {
        ModOpenGl stopped = mods;
        mods = null;
        if (stopped != null) {
            binding.uninstall();
            stopped.close();
        }
    }
}
