package io.github.luasgraphicss.minemod.compatibility.natives.provider;

import io.github.luasgraphicss.minemod.compatibility.contexts.ContextBackend;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import io.github.luasgraphicss.minemod.compatibility.provider.load.LoadedProvider;

final class ProviderContexts implements ContextBackend {
    private static final long NO_CONTEXT = 0;
    private final LoadedProvider loaded;
    private final boolean coreProfile;
    private final int majorVersion;
    private final int minorVersion;

    ProviderContexts(LoadedProvider loaded, boolean coreProfile, int majorVersion, int minorVersion) {
        this.loaded = loaded;
        this.coreProfile = coreProfile;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    @Override
    public long create(long sharedWith) {
        return NativeBinding.createContext(loaded.handle(), coreProfile, majorVersion, minorVersion, sharedWith);
    }

    @Override
    public void makeCurrent(long handle) {
        NativeBinding.makeCurrent(loaded.handle(), handle);
    }

    @Override
    public void releaseCurrent() {
        NativeBinding.makeCurrent(loaded.handle(), NO_CONTEXT);
    }

    @Override
    public void destroy(long handle) {
        NativeBinding.destroyContext(loaded.handle(), handle);
    }
}
