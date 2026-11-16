package io.github.luasgraphicss.minemod.compatibility.natives.provider;

import io.github.luasgraphicss.minemod.compatibility.contexts.ContextBackend;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.FunctionSource;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.LibraryImage;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.load.LoadedProvider;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class NativeProvider {
    private final LoadedProvider loaded;

    public NativeProvider(LoadedProvider loaded) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
    }

    public LibraryImage image() {
        return LibraryImage.ofSegments(NativeBinding.imageSegments(loaded.handle()));
    }

    public FunctionSource functions() {
        return function -> NativeBinding.functionAddress(loaded.handle(), function);
    }

    public ContextBackend contexts(ContextProfile profile, int majorVersion, int minorVersion) {
        if (majorVersion < 1 || minorVersion < 0) {
            throw new IllegalArgumentException("OpenGL " + majorVersion + "." + minorVersion + " is not a version");
        }
        return new ProviderContexts(loaded, profile == ContextProfile.CORE, majorVersion, minorVersion);
    }

    public ProviderCalls calls() {
        return new NativeCalls(loaded);
    }
}
